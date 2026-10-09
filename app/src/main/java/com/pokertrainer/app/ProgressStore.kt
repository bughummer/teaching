package com.pokertrainer.app

import android.content.Context

/** Tracks per-lesson best practice score, nothing fancier. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    fun bestScore(lessonId: String): Int = prefs.getInt(key(lessonId), -1)

    fun recordScore(lessonId: String, correct: Int, total: Int) {
        val pct = if (total == 0) 0 else (correct * 100) / total
        val best = bestScore(lessonId)
        if (pct > best) {
            prefs.edit().putInt(key(lessonId), pct).apply()
        }
    }

    private fun key(lessonId: String) = "best_$lessonId"
}
