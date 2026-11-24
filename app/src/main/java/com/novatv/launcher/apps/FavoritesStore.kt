package com.novatv.launcher.apps

import android.content.Context
import android.content.SharedPreferences
import com.novatv.launcher.model.AppInfo
import com.novatv.launcher.ui.Prefs

/** Persistent list of favorite apps, keyed by flattened component name. */
class FavoritesStore(context: Context) {

    private val prefs: SharedPreferences = Prefs.get(context)
    private var cached: MutableSet<String>? = null

    private fun load(): MutableSet<String> {
        if (cached == null) {
            cached = (prefs.getStringSet(KEY, emptySet()) ?: emptySet()).toMutableSet()
        }
        return cached!!
    }

    fun isFavorite(app: AppInfo): Boolean = load().contains(keyOf(app))

    /** @return true if the app is now a favorite. */
    fun toggle(app: AppInfo): Boolean {
        val set = load()
        val key = keyOf(app)
        val nowFavorite = !set.contains(key)
        if (nowFavorite) set.add(key) else set.remove(key)
        persist()
        return nowFavorite
    }

    private fun persist() {
        prefs.edit().putStringSet(KEY, HashSet(load())).apply()
    }

    companion object {
        private const val KEY = "favorites"
        private fun keyOf(app: AppInfo): String = app.componentName.flattenToString()
    }
}
