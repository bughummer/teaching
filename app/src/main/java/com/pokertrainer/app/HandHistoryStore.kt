package com.pokertrainer.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HandHistoryEntry(val seat: String, val archetype: String, val summary: String, val timestamp: Long)

/** Keeps the last [MAX_ENTRIES] played hands as a simple local log, newest first. */
class HandHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("hand_history", Context.MODE_PRIVATE)

    fun record(seat: String, archetype: String, summary: String) {
        val entries = all().toMutableList()
        entries.add(0, HandHistoryEntry(seat, archetype, summary, System.currentTimeMillis()))
        val trimmed = entries.take(MAX_ENTRIES)
        val array = JSONArray()
        trimmed.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("seat", entry.seat)
                    put("archetype", entry.archetype)
                    put("summary", entry.summary)
                    put("timestamp", entry.timestamp)
                },
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun all(): List<HandHistoryEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            HandHistoryEntry(obj.getString("seat"), obj.getString("archetype"), obj.getString("summary"), obj.getLong("timestamp"))
        }
    }

    companion object {
        private const val KEY = "entries"
        private const val MAX_ENTRIES = 50
    }
}
