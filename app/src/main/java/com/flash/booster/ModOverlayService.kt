
package com.flash.booster

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class ModOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var smoothMode = true

    private val cyan = Color.rgb(0, 220, 255)
    private val dark = Color.rgb(19, 25, 42)
    private val white = Color.WHITE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (panel == null) showPanel()
        return START_NOT_STICKY
    }

    private fun showPanel() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            background = GradientDrawable().apply {
                setColor(dark)
                cornerRadius = 24f
                setStroke(2, cyan)
            }
        }

        val title = TextView(this).apply {
            text = "FLASH 🌙 BOOSTER"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 12)
        }
        layout.addView(title)

        addButton(layout, "🔊 AUDIO SETTINGS") {
            openSettings(Settings.ACTION_SOUND_SETTINGS)
        }

        addButton(layout, "📶 MOBILE NETWORK") {
            openSettings(Settings.ACTION_WIRELESS_SETTINGS)
        }

        addButton(layout, "✨ Smooth Mode: ON") {
            smoothMode = !smoothMode
            Toast.makeText(
                this,
                if (smoothMode) "Smooth Mode ON" else "Smooth Mode OFF",
                Toast.LENGTH_SHORT
            ).show()
        }

        addButton(layout, "✖ CLOSE PANEL") {
            stopSelf()
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 180
        }

        try {
            windowManager.addView(layout, params)
            panel = layout
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Panel open nahi hua: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            stopSelf()
        }
    }

    private fun addButton(
        parent: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            textSize = 12f
            setTextColor(Color.BLACK)
            setBackgroundColor(cyan)
            isAllCaps = false
            setOnClickListener { action() }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 5
            }
        )
    }

    private fun openSettings(action: String) {
        try {
            startActivity(
                Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
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
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        panel = null
        super.onDestroy()
    }
}
