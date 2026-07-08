package com.waterbuddy.app

import android.content.Context
import java.time.LocalDate

/** All persistence lives here: SharedPreferences, shared by the app UI,
 *  the overlay service, and the alarm receiver. No database needed. */
object Store {
    private const val P = "waterbuddy"
    private fun sp(c: Context) = c.getSharedPreferences(P, Context.MODE_PRIVATE)
    private fun today() = LocalDate.now().toString()

    /** Roll yesterday's total into history when the date changes. */
    private fun ensureToday(c: Context) {
        val s = sp(c)
        val d = today()
        val cur = s.getString("todayDate", d)
        if (cur != d) {
            val ml = s.getInt("todayMl", 0)
            s.edit().putInt("day_$cur", ml).putString("todayDate", d).putInt("todayMl", 0).apply()
        } else if (!s.contains("todayDate")) {
            s.edit().putString("todayDate", d).apply()
        }
    }

    // settings
    fun goal(c: Context) = sp(c).getInt("goal", 2000)
    fun setGoal(c: Context, v: Int) = sp(c).edit().putInt("goal", v.coerceIn(500, 5000)).apply()
    fun glass(c: Context) = sp(c).getInt("glass", 250)
    fun setGlass(c: Context, v: Int) = sp(c).edit().putInt("glass", v.coerceIn(100, 1000)).apply()
    fun interval(c: Context) = sp(c).getInt("interval", 60)
    fun setInterval(c: Context, v: Int) = sp(c).edit().putInt("interval", v.coerceIn(15, 240)).apply()
    fun startHour(c: Context) = sp(c).getInt("startHour", 8)
    fun endHour(c: Context) = sp(c).getInt("endHour", 22)
    fun remindersOn(c: Context) = sp(c).getBoolean("remindersOn", false)
    fun setRemindersOn(c: Context, v: Boolean) = sp(c).edit().putBoolean("remindersOn", v).apply()

    // intake
    fun todayMl(c: Context): Int { ensureToday(c); return sp(c).getInt("todayMl", 0) }
    fun addWater(c: Context, amount: Int) {
        ensureToday(c)
        val s = sp(c)
        s.edit()
            .putInt("todayMl", (s.getInt("todayMl", 0) + amount).coerceAtLeast(0))
            .putLong("lastDrinkTs", System.currentTimeMillis())
            .apply()
    }
    fun removeWater(c: Context, amount: Int) {
        ensureToday(c)
        val s = sp(c)
        s.edit().putInt("todayMl", (s.getInt("todayMl", 0) - amount).coerceAtLeast(0)).apply()
    }
    fun resetToday(c: Context) { ensureToday(c); sp(c).edit().putInt("todayMl", 0).apply() }

    fun lastDrinkTs(c: Context) = sp(c).getLong("lastDrinkTs", 0L)
    fun lastRemindTs(c: Context) = sp(c).getLong("lastRemindTs", 0L)
    fun setLastRemindTs(c: Context, v: Long) = sp(c).edit().putLong("lastRemindTs", v).apply()

    fun inActiveHours(c: Context): Boolean {
        val h = java.time.LocalTime.now().hour
        return h >= startHour(c) && h < endHour(c)
    }
    fun goalReached(c: Context) = todayMl(c) >= goal(c)

    // ---- stats ----
    private fun mlOn(c: Context, date: String): Int {
        return if (date == today()) todayMl(c) else sp(c).getInt("day_$date", 0)
    }
    fun currentStreak(c: Context): Int {
        val g = goal(c)
        var streak = 0
        var d = LocalDate.now()
        if (mlOn(c, d.toString()) < g) d = d.minusDays(1) // don't lose streak mid-day
        var i = 0
        while (i < 400) {
            if (mlOn(c, d.toString()) >= g) { streak++; d = d.minusDays(1) } else break
            i++
        }
        return streak
    }
    fun bestStreak(c: Context): Int {
        val g = goal(c)
        val s = sp(c)
        val days = s.all.keys.filter { it.startsWith("day_") }
            .map { it.removePrefix("day_") }
            .filter { (s.getInt("day_$it", 0)) >= g }
            .toMutableList()
        if (mlOn(c, today()) >= g) days.add(today())
        val sorted = days.distinct().sorted()
        var best = 0; var run = 0; var prev: LocalDate? = null
        for (k in sorted) {
            val dt = LocalDate.parse(k)
            run = if (prev != null && prev.plusDays(1) == dt) run + 1 else 1
            best = maxOf(best, run); prev = dt
        }
        return best
    }
    fun last7(c: Context): List<Pair<String, Int>> {
        val out = ArrayList<Pair<String, Int>>()
        for (i in 6 downTo 0) {
            val d = LocalDate.now().minusDays(i.toLong())
            out.add(d.toString() to mlOn(c, d.toString()))
        }
        return out
    }
    fun dailyAverage(c: Context): Int {
        val s = sp(c)
        val vals = s.all.filterKeys { it.startsWith("day_") }.values.map { it as Int }.toMutableList()
        vals.add(todayMl(c))
        return if (vals.isEmpty()) 0 else vals.sum() / vals.size
    }
}
