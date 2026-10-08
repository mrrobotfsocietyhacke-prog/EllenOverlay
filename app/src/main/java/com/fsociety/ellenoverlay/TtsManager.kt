package com.fsociety.ellenoverlay

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(
    context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.language = Locale("ru", "RU")
            tts.setSpeechRate(1.0f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = onSpeakingChanged(true)
                override fun onDone(utteranceId: String?) = onSpeakingChanged(false)
                override fun onError(utteranceId: String?) = onSpeakingChanged(false)
            })
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        onSpeakingChanged(true)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ellen_reply")
    }

    fun stop() {
        tts.stop()
        onSpeakingChanged(false)
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
