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

    private val cyan = Color.rgb(0, 220, 255)
    private val dark = Color.rgb(19, 25, 42)
    private val white = Color.WHITE

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
                this, "Pehle floating permission ON karo",
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
                this, "Panel error: ${e.message}",
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

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setContentTitle("FLASH Booster")
            .setContentText("Floating panel is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                1001,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1001, notification)
        }
    }

    private fun showPanel() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            background = GradientDrawable().apply {
                setColor(dark)
                cornerRadius = 22f
                setStroke(2, cyan)
            }
        }

        val title = TextView(this).apply {
            text = "FLASH 🌙 BOOSTER  ⋮"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(4, 8, 4, 14)
        }
        content.addView(title)

        // Drag the panel by holding its title.
        title.setOnTouchListener(object : View.OnTouchListener {
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
                        p.x = startX - (event.rawX - touchX).toInt()
                        p.y = startY + (event.rawY - touchY).toInt()
                        try {
                            wm.updateViewLayout(panel, p)
                        } catch (_: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> return true
                }
                return false
            }
        })

        addToggle(content, "🔊 Voice Priority", "Voice Priority")
        addToggle(content, "📶 Net Stability", "Net Stability")
        addToggle(content, "⚡ Fast Drop / Send", "Fast Drop / Send")
        addToggle(content, "↵ Enter = Send", "Enter = Send")
        addToggle(content, "✨ Smooth Mode", "Smooth Mode")

        addButton(content, "🔊 OPEN AUDIO SETTINGS") {
            openSettings(Settings.ACTION_SOUND_SETTINGS)
        }

        addButton(content, "📶 OPEN NETWORK SETTINGS") {
            openSettings(Settings.ACTION_WIRELESS_SETTINGS)
        }

        addButton(content, "✖ CLOSE PANEL") {
            stopSelf()
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            addView(content)
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val p = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.78f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 12
            y = 160
        }

        try {
            params = p
            panel = scroll
            wm.addView(scroll, p)
        } catch (e: Exception) {
            panel = null
            params = null
            throw e
        }
    }

    private fun addToggle(
        parent: LinearLayout,
        label: String,
        key: String
    ) {
        val button = Button(this)
        updateToggleLabel(button, label, key)

        button.setOnClickListener {
            enabled[key] = !(enabled[key] ?: false)
            updateToggleLabel(button, label, key)

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
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 5 }
        )
    }

    private fun updateToggleLabel(
        button: Button,
        label: String,
        key: String
    ) {
        val isOn = enabled[key] == true
        button.text = "$label : ${if (isOn) "ON" else "OFF"}"
        button.textSize = 12f
        button.isAllCaps = false
        button.setTextColor(Color.BLACK)
        button.setBackgroundColor(if (isOn) cyan else Color.LTGRAY)
    }

    private fun addButton(
        parent: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.BLACK)
            setBackgroundColor(cyan)
            setOnClickListener { action() }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 5 }
        )
    }

    private fun openSettings(action: String) {
        try {
            startActivity(
                Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            Toast.makeText(
                this, "Settings open nahi ho sakin",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        panel?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {}
        }
        panel = null
        params = null
        super.onDestroy()
    }
}
