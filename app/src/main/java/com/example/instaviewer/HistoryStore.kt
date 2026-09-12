package com.example.instaviewer

import android.content.Context

class HistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("insta_viewer", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "history"
        private const val KEY_DARK = "dark_mode"
        private const val KEY_MODE = "view_mode"
        private const val KEY_SERVICE_URL = "service_url"
        private const val MAX_ITEMS = 20

        const val MODE_DIRECT = 0
        const val MODE_SERVICE = 1
        const val MODE_ACCOUNT = 2
    }

    fun getHistory(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split("|")
    }

    fun add(username: String) {
        val list = getHistory().toMutableList()
        list.remove(username)
        list.add(0, username)
        prefs.edit().putString(KEY_HISTORY, list.take(MAX_ITEMS).joinToString("|")).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    var viewMode: Int
        get() = prefs.getInt(KEY_MODE, MODE_DIRECT)
        set(value) = prefs.edit().putInt(KEY_MODE, value).apply()

    var serviceUrlTemplate: String
        get() = prefs.getString(KEY_SERVICE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SERVICE_URL, value).apply()
}
