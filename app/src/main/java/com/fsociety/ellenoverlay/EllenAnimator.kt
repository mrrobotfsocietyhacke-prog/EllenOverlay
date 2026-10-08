package com.fsociety.ellenoverlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.io.IOException
import kotlin.random.Random

class EllenAnimator(
    private val context: Context,
    private val imageView: ImageView
) {
    enum class Mode { IDLE, TALK, HAPPY, ANGRY, SLEEPY }

    private val handler = Handler(Looper.getMainLooper())
    private val cache = HashMap<String, Bitmap>()
    private var mode = Mode.IDLE
    private var frame = 0
    private var running = false

    private val idle = (1..5).map { "sprites/idle/idle_%02d.png".format(it) }
    private val blink = (1..4).map { "sprites/blink/blink_%02d.png".format(it) }
    private val talk = (1..6).map { "sprites/talk/talk_%02d.png".format(it) }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            when (mode) {
                Mode.IDLE -> playIdleTick()
                Mode.TALK -> playTalkTick()
                Mode.HAPPY -> show("sprites/emotion/happy.png")
                Mode.ANGRY -> show("sprites/emotion/angry.png")
                Mode.SLEEPY -> show("sprites/emotion/sleepy.png")
            }
            handler.postDelayed(this, when (mode) {
                Mode.TALK -> 130L
                Mode.IDLE -> 220L
                else -> 500L
            })
        }
    }

    fun start() {
        if (running) return
        running = true
        frame = 0
        handler.removeCallbacks(tick)
        tick.run()
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
    }

    fun setMode(newMode: Mode) {
        mode = newMode
        frame = 0
        if (!running) start()
    }

    fun idle() = setMode(Mode.IDLE)
    fun talk() = setMode(Mode.TALK)
    fun happy() = setMode(Mode.HAPPY)
    fun angry() = setMode(Mode.ANGRY)
    fun sleepy() = setMode(Mode.SLEEPY)

    private fun playIdleTick() {
        // Mostly idle frames, with an occasional 4-frame blink.
        if (Random.nextInt(100) < 4) {
            blink.forEachIndexed { index, path ->
                handler.postDelayed({ if (running && mode == Mode.IDLE) show(path) }, index * 70L)
            }
            frame = 0
            return
        }
        show(idle[frame % idle.size])
        frame++
    }

    private fun playTalkTick() {
        show(talk[frame % talk.size])
        frame++
    }

    private fun show(path: String) {
        val bitmap = cache.getOrPut(path) { load(path) ?: return }
        imageView.setImageBitmap(bitmap)
    }

    private fun load(path: String): Bitmap? = try {
        context.assets.open(path).use { BitmapFactory.decodeStream(it) }
    } catch (_: IOException) {
        null
    }
}
