package com.novatv.launcher.ui

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import com.novatv.launcher.themes.ThemeManager

/**
 * Inflates layouts with the currently selected launcher theme so that
 * custom theme attributes (?attr/nova*) resolve inside dialogs too.
 */
fun themedInflater(inflater: LayoutInflater): LayoutInflater {
    val theme = ThemeManager(Prefs.get(inflater.context)).current.themeRes
    return inflater.cloneInContext(ContextThemeWrapper(inflater.context, theme))
}

/** Resolves a theme color attribute (?attr) to a concrete int color. */
fun themedColor(ctx: Context, attr: Int): Int {
    val tv = TypedValue()
    ctx.theme.resolveAttribute(attr, tv, true)
    return tv.data
}

fun dpOf(ctx: Context, value: Int): Int =
    (value * ctx.resources.displayMetrics.density).toInt()
