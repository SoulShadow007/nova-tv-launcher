package com.novatv.launcher.settings

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.novatv.launcher.MainActivity
import com.novatv.launcher.R
import com.novatv.launcher.files.FileManagerDialog
import com.novatv.launcher.ui.Anim
import com.novatv.launcher.ui.dpOf
import com.novatv.launcher.ui.themedColor
import com.novatv.launcher.ui.themedInflater

/** Quick wallpaper picker opened from the bottom bar. */
class WallpaperDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.NovaDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = themedInflater(inflater).inflate(R.layout.dialog_wallpaper, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val container = view.findViewById<LinearLayout>(R.id.wallpaper_container)
        val ctx = container.context
        val activity = requireActivity() as MainActivity

        val title = TextView(ctx).apply {
            text = getString(R.string.wallpaper_title)
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(themedColor(ctx, R.attr.novaTextPrimary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpOf(ctx, 22) }
        }
        container.addView(title)

        fun addOption(label: String, onClick: () -> Unit) {
            val option = TextView(ctx).apply {
                text = label
                textSize = 17f
                setTextColor(themedColor(ctx, R.attr.novaTextPrimary))
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_button)
                setPadding(dpOf(ctx, 18), dpOf(ctx, 14), dpOf(ctx, 18), dpOf(ctx, 14))
                isFocusable = true
                isClickable = true
                setOnClickListener { onClick() }
                onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    Anim.focus(v, hasFocus)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpOf(ctx, 10) }
            }
            container.addView(option)
        }

        addOption(getString(R.string.wallpaper_pick_image)) {
            dismiss()
            activity.showFileManager(FileManagerDialog.MODE_IMAGE)
        }
        addOption(getString(R.string.wallpaper_pick_video)) {
            dismiss()
            activity.showFileManager(FileManagerDialog.MODE_VIDEO)
        }
        addOption(getString(R.string.wallpaper_reset)) {
            activity.resetWallpaper()
            dismiss()
        }

        val hint = TextView(ctx).apply {
            text = getString(R.string.wallpaper_hint)
            textSize = 14f
            setTextColor(themedColor(ctx, R.attr.novaTextSecondary))
            setLineSpacing(0f, 1.25f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpOf(ctx, 10) }
        }
        container.addView(hint)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
