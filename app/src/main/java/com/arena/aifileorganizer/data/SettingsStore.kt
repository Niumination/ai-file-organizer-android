package com.arena.aifileorganizer.data

import android.content.Context

/**
 * Non-secret app settings (plain SharedPreferences is fine — the persisted
 * URI string is useless on another device without the SAF permission grant,
 * which lives in the system, not here).
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val PREFS_NAME = "ai_organizer_settings"
        const val KEY_TREE_URI = "tree_uri"
    }

    var treeUri: String?
        get() = prefs.getString(KEY_TREE_URI, null)
        set(value) {
            if (value == null) prefs.edit().remove(KEY_TREE_URI).apply()
            else prefs.edit().putString(KEY_TREE_URI, value).apply()
        }

    fun clear() = prefs.edit().clear().apply()
}
