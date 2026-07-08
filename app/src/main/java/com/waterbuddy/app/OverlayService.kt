package com.waterbuddy.app

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/** Owns the floating character window. Started by the alarm receiver (or the
 *  "preview" button). Walks the buddy in from the edge, waves, waits for the
 *  user to confirm a drink, then walks off and stops itself. */
class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "show"
        const val ACTION_HIDE = "hide"
        private const val CHANNEL = "buddy_overlay"
        private const val NOTIF_ID = 42
    }

    private lateinit var wm: WindowManager
    private var root: View? = null
    private lateinit var lp: WindowManager.LayoutParams
    private val h = Handler(Looper.getMainLooper())
    private var shown = false
    private var leaving = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun screenW() = resources.displayMetrics.widthPixels
    private fun screenH() = resources.displayMetrics.heightPixels

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> walkOutAndStop()
            else -> showBuddy()
        }
        return START_NOT_STICKY
    }

    // ---------- foreground plumbing ----------
    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Floating buddy", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("Water Buddy 💧")
            .setContentText("Tap the buddy when you've had some water")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(open)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    // ---------- show / walk-in ----------
    private fun showBuddy() {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        if (shown) { wave(); return }
        leaving = false

        val v = LayoutInflater.from(this).inflate(R.layout.overlay_buddy, null)
        root = v
        val buddy = v.findViewById<ImageView>(R.id.buddy)
        val speech = v.findViewById<TextView>(R.id.speech)
        val panel = v.findViewById<LinearLayout>(R.id.panel)

        Buddy.play(buddy, Buddy.WALK)

        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.BOTTOM
            x = -dp(220)
            y = dp(90)
        }

        try { wm.addView(v, lp) } catch (e: Exception) { stopSelf(); return }
        shown = true

        setupTouch(buddy, panel, speech)
        v.findViewById<TextView>(R.id.btnDrank).setOnClickListener { onDrank(buddy) }
        v.findViewById<TextView>(R.id.btnSnooze).setOnClickListener { onSnooze() }

        // walk in from the left edge to a resting spot
        val restX = dp(16)
        ValueAnimator.ofInt(-dp(220), restX).apply {
            duration = 2200
            addUpdateListener {
                lp.x = it.animatedValue as Int
                safeUpdate()
            }
            addListener(onEnd = {
                if (shown && !leaving) {
                    Buddy.play(buddy, Buddy.WAVE)
                    speech.text = pickLine()
                    speech.visibility = View.VISIBLE
                    h.postDelayed({ if (shown) buddy.let { Buddy.play(it, Buddy.IDLE) } }, 3200)
                }
            })
            start()
        }
    }

    private fun wave() {
        val v = root ?: return
        val buddy = v.findViewById<ImageView>(R.id.buddy)
        val speech = v.findViewById<TextView>(R.id.speech)
        Buddy.play(buddy, Buddy.WAVE)
        speech.text = pickLine()
        speech.visibility = View.VISIBLE
        h.postDelayed({ if (shown) Buddy.play(buddy, Buddy.IDLE) }, 3200)
    }

    private fun pickLine(): String = listOf(
        "Time to drink 💧", "Hey, water break!", "Your buddy is thirsty…", "Let's hydrate 💦"
    ).random()

    // ---------- touch: drag + tap-to-expand ----------
    private var downX = 0f; private var downY = 0f
    private var startXParam = 0; private var startYParam = 0
    private var downTime = 0L; private var dragged = false

    private fun setupTouch(buddy: ImageView, panel: LinearLayout, speech: TextView) {
        buddy.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startXParam = lp.x; startYParam = lp.y
                    downTime = System.currentTimeMillis(); dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downX).toInt()
                    val dy = (e.rawY - downY).toInt()
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) dragged = true
                    lp.x = (startXParam + dx).coerceIn(-dp(20), screenW() - dp(90))
                    lp.y = (startYParam - dy).coerceIn(0, screenH() - dp(120))
                    safeUpdate()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val quick = System.currentTimeMillis() - downTime < 250
                    if (!dragged && quick) {
                        val opening = panel.visibility != View.VISIBLE
                        panel.visibility = if (opening) View.VISIBLE else View.GONE
                        if (opening) speech.visibility = View.GONE
                    }
                    true
                }
                else -> false
            }
        }
    }

    // ---------- actions ----------
    private fun onDrank(buddy: ImageView) {
        Store.addWater(this, Store.glass(this))
        root?.findViewById<LinearLayout>(R.id.panel)?.visibility = View.GONE
        val speech = root?.findViewById<TextView>(R.id.speech)
        speech?.text = "Thank you! 💙"
        speech?.visibility = View.VISIBLE
        Buddy.play(buddy, Buddy.DRINK, loop = false)
        h.postDelayed({ walkOutAndStop() }, 1800)
    }

    private fun onSnooze() {
        Scheduler.scheduleNext(this, minutesFromNow = 10)
        walkOutAndStop()
    }

    private fun walkOutAndStop() {
        if (leaving) return
        leaving = true
        val v = root
        if (v == null) { stopSelf(); return }
        val buddy = v.findViewById<ImageView>(R.id.buddy)
        v.findViewById<LinearLayout>(R.id.panel).visibility = View.GONE
        v.findViewById<TextView>(R.id.speech).visibility = View.GONE
        Buddy.play(buddy, Buddy.WALK)
        ValueAnimator.ofInt(lp.x, screenW() + dp(60)).apply {
            duration = 1600
            addUpdateListener { lp.x = it.animatedValue as Int; safeUpdate() }
            addListener(onEnd = { removeAndStop() })
            start()
        }
    }

    private fun removeAndStop() {
        try { root?.let { wm.removeView(it) } } catch (_: Exception) {}
        root = null; shown = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun safeUpdate() {
        try { root?.let { if (it.isAttachedToWindow) wm.updateViewLayout(it, lp) } } catch (_: Exception) {}
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        try { root?.let { wm.removeView(it) } } catch (_: Exception) {}
        root = null; shown = false
        super.onDestroy()
    }
}

/* tiny extension so we can pass a lambda to Animator.end without the verbose listener */
private fun ValueAnimator.addListener(onEnd: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) { onEnd() }
    })
}
