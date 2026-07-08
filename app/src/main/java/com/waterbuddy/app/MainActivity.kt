package com.waterbuddy.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.waterbuddy.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var updating = false

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnDrink.setOnClickListener { drink(Store.glass(this)) }
        b.qHalf.setOnClickListener { drink(Store.glass(this) / 2) }
        b.qBig.setOnClickListener { drink(Store.glass(this) * 2) }
        b.qUndo.setOnClickListener { Store.removeWater(this, Store.glass(this)); refresh() }

        // steppers
        b.goalMinus.setOnClickListener { Store.setGoal(this, Store.goal(this) - 250); refresh() }
        b.goalPlus.setOnClickListener { Store.setGoal(this, Store.goal(this) + 250); refresh() }
        b.glassMinus.setOnClickListener { Store.setGlass(this, Store.glass(this) - 50); refresh() }
        b.glassPlus.setOnClickListener { Store.setGlass(this, Store.glass(this) + 50); refresh() }
        b.intMinus.setOnClickListener { Store.setInterval(this, Store.interval(this) - 15); rescheduleIfOn(); refresh() }
        b.intPlus.setOnClickListener { Store.setInterval(this, Store.interval(this) + 15); rescheduleIfOn(); refresh() }

        b.btnOverlayPerm.setOnClickListener { requestOverlay() }
        b.btnNotifPerm.setOnClickListener { requestNotif() }
        b.btnPreview.setOnClickListener { preview() }

        b.switchReminders.setOnCheckedChangeListener { _, isChecked ->
            if (updating) return@setOnCheckedChangeListener
            if (isChecked) enableReminders() else disableReminders()
        }
    }

    override fun onResume() { super.onResume(); refresh() }

    // ---------- intake ----------
    private fun drink(amount: Int) {
        Store.addWater(this, amount)
        Buddy.play(b.homeBuddy, Buddy.DRINK, loop = false)
        b.homeBuddy.postDelayed({ refreshBuddy() }, 1700)
        refresh()
    }

    // ---------- permissions ----------
    private fun hasOverlay() = Settings.canDrawOverlays(this)
    private fun hasNotif() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestOverlay() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    }
    private fun requestNotif() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotif()) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Toast.makeText(this, "Notifications already allowed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun preview() {
        if (!hasOverlay()) {
            Toast.makeText(this, "First allow drawing over other apps", Toast.LENGTH_SHORT).show()
            requestOverlay(); return
        }
        val svc = Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_SHOW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc) else startService(svc)
        Toast.makeText(this, "Here she comes 💧", Toast.LENGTH_SHORT).show()
    }

    // ---------- reminders ----------
    private fun enableReminders() {
        if (!hasOverlay()) {
            Toast.makeText(this, "Allow drawing over other apps first", Toast.LENGTH_LONG).show()
            requestOverlay(); setSwitch(false); return
        }
        if (!hasNotif()) { requestNotif() }
        Store.setRemindersOn(this, true)
        Scheduler.scheduleNext(this)
        refresh()
    }
    private fun disableReminders() {
        Store.setRemindersOn(this, false)
        Scheduler.cancel(this)
        refresh()
    }
    private fun rescheduleIfOn() { if (Store.remindersOn(this)) Scheduler.scheduleNext(this) }
    private fun setSwitch(on: Boolean) { updating = true; b.switchReminders.isChecked = on; updating = false }

    // ---------- render ----------
    private fun refresh() {
        val goal = Store.goal(this)
        val ml = Store.todayMl(this)
        b.progressText.text = "$ml / $goal ml"
        b.bar.progress = if (goal == 0) 0 else (ml * 100 / goal).coerceIn(0, 100)

        val left = (goal - ml).coerceAtLeast(0)
        b.subText.text = if (left == 0) "Goal reached, amazing 🎉"
            else "${Math.round(left.toDouble() / Store.glass(this))} more glasses to go"

        b.streakPill.text = "🔥 ${Store.currentStreak(this)}"
        b.goalValue.text = "$goal ml"
        b.glassValue.text = "${Store.glass(this)} ml"
        b.intValue.text = "${Store.interval(this)} min"

        setSwitch(Store.remindersOn(this))
        b.reminderHint.text = when {
            !hasOverlay() -> "Needs 'draw over other apps' permission"
            Store.remindersOn(this) -> "On • buddy visits every ${Store.interval(this)} min"
            else -> "Off"
        }
        b.btnOverlayPerm.text = if (hasOverlay()) "✓ Drawing over apps allowed" else "1. Allow drawing over other apps"
        b.btnNotifPerm.text = if (hasNotif()) "✓ Notifications allowed" else "2. Allow notifications"

        b.statsText.text = buildStats()
        refreshBuddy()
    }

    private fun refreshBuddy() {
        if (Store.goalReached(this)) Buddy.play(b.homeBuddy, Buddy.WAVE)
        else Buddy.play(b.homeBuddy, Buddy.IDLE)
    }

    private fun buildStats(): String {
        val goal = Store.goal(this)
        val blocks = "▁▂▃▄▅▆▇█"
        val spark = Store.last7(this).joinToString(" ") { (_, v) ->
            val idx = if (goal == 0) 0 else (v * (blocks.length - 1) / goal).coerceIn(0, blocks.length - 1)
            blocks[idx].toString()
        }
        return "Current streak:  ${Store.currentStreak(this)} days\n" +
               "Best streak:  ${Store.bestStreak(this)} days\n" +
               "Daily average:  ${Store.dailyAverage(this)} ml\n" +
               "Last 7 days:  $spark"
    }
}
