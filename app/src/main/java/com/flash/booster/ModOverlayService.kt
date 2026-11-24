package com.flash.booster

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class ModOverlayService : Service() {

    private lateinit var wm: WindowManager
    private var panel: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var collapsed = false
    private var compact = true

    private val cyan = Color.rgb(0, 220, 255)
    private val dark = Color.rgb(19, 25, 42)

    private val enabled = mutableMapOf(
        "Voice Priority" to false,
        "Net Stability" to false,
        "Fast Drop / Send" to false,
        "Enter = Send" to false,
        "Smooth Mode" to true
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Pehle floating permission ON karo",
                Toast.LENGTH_LONG
            ).show()
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            startAsForeground()
            if (panel == null) showPanel()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Panel error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            stopSelf()
        }

        return START_STICKY
    }

    private fun startAsForeground() {
        val channelId = "flash_booster_service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FLASH Booster",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle("FLASH Booster")
            .setContentText("Floating panel is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                1001,
                notification,
                android.content.pm.ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1001, notification)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun showPanel() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            dp(230),
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(8)
            y = dp(150)
        }

        renderPanel()
    }

    private fun renderPanel() {
        val p = params ?: return

        panel?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        panel = null

        if (collapsed) {
            val bubble = TextView(this).apply {
                text = "🌙"
                textSize = 25f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(cyan)
                    setStroke(dp(2), Color.WHITE)
                }
                setOnClickListener {
                    collapsed = false
                    renderPanel()
                }
            }

            p.width = dp(54)
            p.height = dp(54)
            panel = bubble
            wm.addView(bubble, p)
            return
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(7), dp(8), dp(7))
            background = GradientDrawable().apply {
                setColor(dark)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(2), cyan)
            }
        }

        val title = TextView(this).apply {
            text = "FLASH 🌙 BOOSTER"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(8), dp(3), dp(8))
        }
        content.addView(title)
        makeDraggable(title)

        addButton(content, "⇤ SIDE HIDE", cyan) {
            collapsed = true
            renderPanel()
        }

        addButton(
            content,
            if (compact) "⤢ RESIZE: SMALL" else "⤡ RESIZE: LARGE",
            cyan
        ) {
            compact = !compact
            renderPanel()
        }

        addToggle(content, "🔊 Voice Priority", "Voice Priority")
        addToggle(content, "📶 Net Stability", "Net Stability")
        addToggle(content, "⚡ Fast Drop / Send", "Fast Drop / Send")
        addToggle(content, "↵ Enter = Send", "Enter = Send")
        addToggle(content, "✨ Smooth Mode", "Smooth Mode")

        addButton(content, "🔊 AUDIO SETTINGS", cyan) {
            openSettings(Settings.ACTION_SOUND_SETTINGS)
        }

        addButton(content, "📶 NETWORK SETTINGS", cyan) {
            openSettings(Settings.ACTION_WIRELESS_SETTINGS)
        }

        addButton(content, "✖ CLOSE PANEL", Color.LTGRAY) {
            stopSelf()
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            addView(content)
        }

        p.width = dp(if (compact) 230 else 290)
        p.height = dp(if (compact) 360 else 500)

        panel = scroll
        wm.addView(scroll, p)
    }

    private fun makeDraggable(view: View) {
        view.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val p = params ?: return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = p.x
                        startY = p.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        p.x = (startX - (event.rawX - touchX)).toInt()
                        p.y = (startY + (event.rawY - touchY)).toInt()

                        try {
                            wm.updateViewLayout(panel, p)
                        } catch (_: Exception) {
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> return true
                }
                return true
            }
        })
    }

    private fun addToggle(
        parent: LinearLayout,
        label: String,
        key: String
    ) {
        val button = Button(this)
        updateToggle(button, label, key)

        button.setOnClickListener {
            enabled[key] = !(enabled[key] ?: false)
            updateToggle(button, label, key)

            Toast.makeText(
                this,
                "$key: ${if (enabled[key] == true) "ON" else "OFF"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(if (compact) 43 else 48)
            ).apply {
                bottomMargin = dp(3)
            }
        )
    }

    private fun updateToggle(
        button: Button,
        label: String,
        key: String
    ) {
        val on = enabled[key] == true
        button.text = "$label : ${if (on) "ON" else "OFF"}"
        button.textSize = if (compact) 11f else 13f
        button.isAllCaps = false
        button.setTextColor(Color.BLACK)
        button.setBackgroundColor(if (on) cyan else Color.LTGRAY)
    }

    private fun addButton(
        parent: LinearLayout,
        label: String,
        color: Int,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            textSize = if (compact) 11f else 13f
            isAllCaps = false
            setTextColor(Color.BLACK)
            setBackgroundColor(color)
            setPadding(dp(3), 0, dp(3), 0)
            setOnClickListener { action() }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(if (compact) 40 else 46)
            ).apply {
                bottomMargin = dp(3)
            }
        )
    }

    private fun openSettings(action: String) {
        try {
            startActivity(
                Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Settings open nahi ho sakin",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        panel?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        panel = null
        params = null
        super.onDestroy()
    }
}
