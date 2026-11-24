package com.novatv.launcher.ui

import android.view.View
import android.view.animation.DecelerateInterpolator
import com.novatv.launcher.R

enum class AnimationStyle(val key: String, val labelRes: Int) {
    SMOOTH("smooth", R.string.anim_smooth),
    SLIDE("slide", R.string.anim_slide),
    ZOOM("zoom", R.string.anim_zoom);

    companion object {
        fun byKey(key: String?): AnimationStyle =
            values().firstOrNull { it.key == key } ?: SMOOTH
    }
}

object Anim {

    /** Focused items grow slightly, giving TV remotes clear visual feedback. */
    fun focus(view: View, hasFocus: Boolean) {
        val target = if (hasFocus) 1.12f else 1f
        view.animate()
            .scaleX(target)
            .scaleY(target)
            .setDuration(140)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /** Transition styles for the preview panel and dialogs. */
    fun appear(view: View, style: AnimationStyle) {
        view.visibility = View.VISIBLE
        view.animate().cancel()
        when (style) {
            AnimationStyle.SMOOTH -> {
                view.alpha = 0f
                view.animate().alpha(1f).setDuration(220)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
            AnimationStyle.SLIDE -> {
                view.translationY = 40f
                view.alpha = 0f
                view.animate().translationY(0f).alpha(1f).setDuration(260)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
            AnimationStyle.ZOOM -> {
                view.scaleX = 0.92f
                view.scaleY = 0.92f
                view.alpha = 0f
                view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
        }
    }

    fun disappear(view: View) {
        view.animate().alpha(0f).setDuration(180).withEndAction {
            view.visibility = View.GONE
            view.alpha = 1f
        }.start()
    }
}
