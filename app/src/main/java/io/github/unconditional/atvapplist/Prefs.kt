package io.github.unconditional.atvapplist

import android.content.Context

internal class Prefs(context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var showSystemApps: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SYSTEM, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_SYSTEM, value).apply()
        }

    var sortMode: SortMode
        get() = try {
            val raw = prefs.getString(KEY_SORT_MODE, null) ?: SortMode.NAME.name
            SortMode.valueOf(raw)
        } catch (e: IllegalArgumentException) {
            SortMode.NAME
        }
        set(value) {
            prefs.edit().putString(KEY_SORT_MODE, value.name).apply()
        }

    fun hiddenPackages(): Set<String> =
        prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()

    fun isHidden(packageName: String): Boolean = hiddenPackages().contains(packageName)

    fun hide(packageName: String) {
        val next = hiddenPackages().toMutableSet()
        next.add(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN, next).apply()
    }

    fun unhide(packageName: String) {
        val next = hiddenPackages().toMutableSet()
        next.remove(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN, next).apply()
    }

    fun unhideAll() {
        prefs.edit().putStringSet(KEY_HIDDEN, emptySet()).apply()
    }

    companion object {
        private const val KEY_SHOW_SYSTEM = "show_system"
        private const val KEY_SORT_MODE = "sort_mode"
        private const val KEY_HIDDEN = "hidden"
    }
}
