package com.fsociety.ellenoverlay

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ChatActivity : Activity() {
    private lateinit var prefs: AppPrefs
    private lateinit var tts: TtsManager
    private val api = ApiClient()
    private val history = mutableListOf<Pair<String, String>>()
    private lateinit var chatBox: LinearLayout
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        tts = TtsManager(this) { speaking ->
            runOnUiThread {
                OverlayService.mood(if (speaking) OverlayService.Mood.TALK else OverlayService.Mood.IDLE)
            }
        }
        buildUi()
        requestNotificationPermission()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.rgb(12, 12, 12))
        }

        val title = TextView(this).apply {
            this.text = "ELLEN // FSOCIETY"
            textSize = 20f
            setTextColor(Color.rgb(230, 70, 70))
            setPadding(0, 0, 0, 12)
        }
        root.addView(title)

        chatBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(chatBox) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        input = EditText(this).apply {
            hint = "Напиши Эллен..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(false)
        }
        root.addView(input, LinearLayout.LayoutParams(-1, -2))

        root.addView(Button(this).apply {
            text = "ОТПРАВИТЬ"
            setOnClickListener { sendMessage() }
        })

        root.addView(Button(this).apply {
            text = "НАСТРОЙКИ / OVERLAY"
            setOnClickListener { showSettings() }
        })

        root.addView(Button(this).apply {
            text = "ЗАПУСТИТЬ OVERLAY"
            setOnClickListener { startOrRequestOverlay() }
        })

        root.addView(Button(this).apply {
            text = "ОСТАНОВИТЬ OVERLAY"
            setOnClickListener { stopService(Intent(this@ChatActivity, OverlayService::class.java)) }
        })

        setContentView(root)
    }

    private fun addMessage(who: String, text: String) {
        chatBox.addView(TextView(this).apply {
            this.text = "$who\n$text\n"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(0, 10, 0, 10)
        })
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isBlank()) return
        input.setText("")
        addMessage("YOU", text)
        history.add("user" to text)
        OverlayService.mood(OverlayService.Mood.TALK)

        val reply = TextView(this).apply {
            this.text = "ELLEN\n..."
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 10, 0, 10)
        }
        chatBox.addView(reply)

        Thread {
            try {
                val answer = kotlinx.coroutines.runBlocking {
                    api.chat(prefs.baseUrl, prefs.apiKey, prefs.model, prefs.systemPrompt, history)
                }
                runOnUiThread {
                    reply.text = "ELLEN\n$answer\n"
                    history.add("assistant" to answer)
                    OverlayService.mood(OverlayService.Mood.HAPPY)
                    tts.speak(answer)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    reply.text = "ELLEN\nОшибка: ${e.message ?: "неизвестная ошибка"}\n"
                    history.removeLastOrNull()
                    OverlayService.mood(OverlayService.Mood.ANGRY)
                }
            }
        }.start()
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        fun field(label: String, value: String): EditText {
            box.addView(TextView(this).apply {
                text = label
                setTextColor(Color.LTGRAY)
            })
            return EditText(this).apply {
                setText(value)
                setTextColor(Color.WHITE)
                setSingleLine(false)
                box.addView(this)
            }
        }

        val key = field("API KEY", prefs.apiKey)
        val base = field("BASE URL", prefs.baseUrl)
        val model = field("MODEL", prefs.model)
        val prompt = field("SYSTEM PROMPT", prefs.systemPrompt)

        val scaleLabel = TextView(this).apply {
            text = "Размер overlay: ${(prefs.overlayScale * 100).toInt()}%"
            setTextColor(Color.LTGRAY)
        }
        box.addView(scaleLabel)

        val scale = SeekBar(this).apply {
            max = 120
            progress = ((prefs.overlayScale - 0.3f) / 1.2f * 120f).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val value = 0.3f + (p / 120f) * 1.2f
                    scaleLabel.text = "Размер overlay: ${(value * 100).toInt()}%"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        box.addView(scale)

        box.addView(Button(this).apply {
            text = "СОХРАНИТЬ"
            setOnClickListener {
                prefs.apiKey = key.text.toString().trim()
                prefs.baseUrl = base.text.toString().trim()
                prefs.model = model.text.toString().trim()
                prefs.systemPrompt = prompt.text.toString()
                prefs.overlayScale = 0.3f + (scale.progress / 120f) * 1.2f
                android.widget.Toast.makeText(this@ChatActivity, "Сохранено", android.widget.Toast.LENGTH_SHORT).show()
            }
        })

        box.addView(Button(this).apply {
            text = "OVERLAY PERMISSION"
            setOnClickListener { openOverlaySettings() }
        })

        android.app.AlertDialog.Builder(this)
            .setTitle("EllenOverlay settings")
            .setView(box)
            .setNegativeButton("ЗАКРЫТЬ", null)
            .show()
    }

    private fun startOrRequestOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings()
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
    }

    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
