
package com.flash.booster

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val bg = Color.rgb(15, 19, 32)
    private val cyan = Color.rgb(0, 220, 255)
    private val white = Color.WHITE

    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 30, 24, 24)
            setBackgroundColor(bg)
        }

        val scroll = android.widget.ScrollView(this)
        scroll.addView(root)

        val title = TextView(this).apply {
            text = "FLASH 🌙 BOOSTER"
            textSize = 27f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 8)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "SMART FLOATING CONTROL"
            textSize = 13f
            setTextColor(white)
            gravity = Gravity.CENTER
        }
        root.addView(subtitle)

        addFeature(
            "🔊 Voice Priority",
            "Quick access to audio settings. Cannot override another app's audio priority."
        )

        addFeature(
            "📶 Mobile Net Stability",
            "Android cannot guarantee extra speed. Keep your signal strong and reduce unnecessary background use."
        )

        addFeature(
            "⚡ Fast Drop / Send",
            "Accessibility support will be configured in a later step."
        )

        addFeature(
            "↵ Enter = Send",
            "Depends on the Yalla input field and its supported actions."
        )

        addFeature(
            "✨ Smooth Mode",
            "Keep controls lightweight and responsive."
        )

        val overlayButton = makeButton("ALLOW FLOATING WINDOW")
        overlayButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    this,
                    "Floating permission already allowed",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
        root.addView(overlayButton, buttonParams())

        val panelButton = makeButton("START FLOATING PANEL")
        panelButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    this,
                    "Pehle floating permission allow karo",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            startService(Intent(this, ModOverlayService::class.java))
            Toast.makeText(
                this,
                "Floating panel starting",
                Toast.LENGTH_SHORT
            ).show()
        }
        root.addView(panelButton, buttonParams())

        setContentView(scroll)
    }

    private fun addFeature(name: String, description: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            setBackgroundColor(Color.rgb(29, 36, 53))
        }

        val heading = TextView(this).apply {
            text = name
            textSize = 18f
            setTextColor(cyan)
        }
        card.addView(heading)

        val detail = TextView(this).apply {
            text = description
            textSize = 13f
            setTextColor(white)
            setPadding(0, 6, 0, 0)
        }
        card.addView(detail)

        root.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 14, 0, 0)
            }
        )
    }

    private fun makeButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.BLACK)
            setBackgroundColor(cyan)
        }
    }

    private fun buttonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 18, 0, 0)
        }
    }
}
