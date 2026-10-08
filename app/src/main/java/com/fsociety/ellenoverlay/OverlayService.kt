package com.fsociety.ellenoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var imageView: ImageView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var prefs: AppPrefs
    private lateinit var animator: EllenAnimator

    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var moved = false

    override fun onCreate() {
        super.onCreate()
        prefs = AppPrefs(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        showOverlay()
        instance = this
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        imageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setOnTouchListener { _, event -> handleTouch(event) }
        }
        animator = EllenAnimator(this, imageView)

        val size = (180 * prefs.overlayScale).toInt().coerceAtLeast(72)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            size,
            size,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.overlayX
            y = prefs.overlayY
        }

        windowManager.addView(imageView, params)
        animator.start()
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                startX = params.x
                startY = params.y
                moved = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - downX).toInt()
                val dy = (event.rawY - downY).toInt()
                if (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6) moved = true
                params.x = startX + dx
                params.y = startY + dy
                windowManager.updateViewLayout(imageView, params)
                return true
            }
            MotionEvent.ACTION_UP -> {
                prefs.overlayX = params.x
                prefs.overlayY = params.y
                if (!moved) {
                    val intent = Intent(this, ChatActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                return true
            }
        }
        return false
    }

    fun setMood(mood: Mood) {
        if (!::animator.isInitialized) return
        when (mood) {
            Mood.IDLE -> animator.idle()
            Mood.TALK -> animator.talk()
            Mood.HAPPY -> animator.happy()
            Mood.ANGRY -> animator.angry()
            Mood.SLEEPY -> animator.sleepy()
        }
    }

    override fun onDestroy() {
        instance = null
        if (::animator.isInitialized) animator.stop()
        if (::imageView.isInitialized) runCatching { windowManager.removeView(imageView) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EllenOverlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Постоянный overlay Эллен" }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, ChatActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("EllenOverlay")
                .setContentText("Эллен активна")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pending)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("EllenOverlay")
                .setContentText("Эллен активна")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pending)
                .setOngoing(true)
                .build()
        }
    }

    enum class Mood { IDLE, TALK, HAPPY, ANGRY, SLEEPY }

    companion object {
        const val CHANNEL_ID = "ellen_overlay"
        const val NOTIFICATION_ID = 1001
        private var instance: OverlayService? = null

        fun mood(mood: Mood) {
            instance?.setMood(mood)
        }
    }
}
