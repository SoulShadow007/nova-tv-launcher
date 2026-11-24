package com.novatv.launcher.ui

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    const val FILE = "nova_launcher"

    const val KEY_THEME = "theme"
    const val KEY_ANIM = "animation"
    const val KEY_CITY = "city"
    const val KEY_WALL_TYPE = "wallpaper_type"
    const val KEY_WALL_PATH = "wallpaper_path"

    const val REQ_FILE_PICK = "nova.request.file_pick"
    const val REQ_APPS_CHANGED = "nova.request.apps_changed"
    const val REQ_CITY_CHANGED = "nova.request.city_changed"

    const val EXTRA_PATH = "nova.extra.path"
    const val EXTRA_TYPE = "nova.extra.type"

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
