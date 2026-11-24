package com.novatv.launcher.settings

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
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
import com.novatv.launcher.themes.LauncherTheme
import com.novatv.launcher.themes.ThemeManager
import com.novatv.launcher.ui.Anim
import com.novatv.launcher.ui.AnimationStyle
import com.novatv.launcher.ui.Prefs
import com.novatv.launcher.ui.dpOf
import com.novatv.launcher.ui.themedColor
import com.novatv.launcher.ui.themedInflater
import com.novatv.launcher.weather.Cities

/** Popup-style launcher settings: theme, wallpaper, weather city, animations, about. */
class SettingsDialog : DialogFragment() {

    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.NovaDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = themedInflater(inflater).inflate(R.layout.dialog_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view
        buildUi()
    }

    private fun buildUi() {
        val container = rootView.findViewById<LinearLayout>(R.id.settings_container)
        container.removeAllViews()
        val ctx = container.context
        val prefs = Prefs.get(requireContext())
        val themeManager = ThemeManager(prefs)
        val activity = requireActivity() as MainActivity

        container.addView(title(ctx, getString(R.string.settings_title)))

        // ---- Theme
        container.addView(section(ctx, getString(R.string.section_theme)))
        LauncherTheme.values().forEach { theme ->
            container.addView(
                option(ctx, getString(theme.labelRes), themeManager.current == theme) {
                    themeManager.apply(theme)
                    dismiss()
                    activity.recreate()
                }
            )
        }

        // ---- Wallpaper
        container.addView(section(ctx, getString(R.string.section_wallpaper)))
        container.addView(
            option(ctx, getString(R.string.wallpaper_pick_image), false) {
                activity.showFileManager(FileManagerDialog.MODE_IMAGE)
            }
        )
        container.addView(
            option(ctx, getString(R.string.wallpaper_pick_video), false) {
                activity.showFileManager(FileManagerDialog.MODE_VIDEO)
            }
        )
        container.addView(
            option(ctx, getString(R.string.wallpaper_reset), false) {
                activity.resetWallpaper()
                dismiss()
            }
        )

        // ---- Weather city
        container.addView(section(ctx, getString(R.string.section_weather)))
        val currentCity = Cities.byId(prefs.getString(Prefs.KEY_CITY, null))
        Cities.ALL.forEach { city ->
            container.addView(
                option(ctx, city.name, city == currentCity) {
                    prefs.edit().putString(Prefs.KEY_CITY, city.id).apply()
                    parentFragmentManager.setFragmentResult(Prefs.REQ_CITY_CHANGED, Bundle())
                    buildUi()
                }
            )
        }

        // ---- Animations
        container.addView(section(ctx, getString(R.string.section_animations)))
        val currentAnim = AnimationStyle.byKey(prefs.getString(Prefs.KEY_ANIM, null))
        AnimationStyle.values().forEach { style ->
            container.addView(
                option(ctx, getString(style.labelRes), style == currentAnim) {
                    prefs.edit().putString(Prefs.KEY_ANIM, style.key).apply()
                    buildUi()
                }
            )
        }

        // ---- About
        container.addView(section(ctx, getString(R.string.section_about)))
        container.addView(
            TextView(ctx).apply {
                text = getString(R.string.about_text)
                textSize = 15f
                setTextColor(themedColor(ctx, R.attr.novaTextSecondary))
                setLineSpacing(0f, 1.25f)
                layoutParams = params(bottom = 0)
            }
        )
    }

    // ---- programmatic view helpers ------------------------------------

    private fun title(ctx: Context, text: String): TextView = TextView(ctx).apply {
        this.text = text
        textSize = 30f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(themedColor(ctx, R.attr.novaTextPrimary))
        layoutParams = params(bottom = 22)
    }

    private fun section(ctx: Context, text: String): TextView = TextView(ctx).apply {
        this.text = text
        textSize = 13f
        setAllCaps(true)
        setTextColor(themedColor(ctx, R.attr.novaAccent))
        layoutParams = params(top = 18, bottom = 10)
    }

    private fun option(
        ctx: Context,
        text: String,
        selected: Boolean,
        onClick: () -> Unit
    ): TextView = TextView(ctx).apply {
        this.text = if (selected) "✓  $text" else "    $text"
        textSize = 17f
        setTypeface(typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
        setTextColor(
            themedColor(ctx, if (selected) R.attr.novaAccent else R.attr.novaTextPrimary)
        )
        background = ContextCompat.getDrawable(ctx, R.drawable.bg_button)
        setPadding(dpOf(ctx, 18), dpOf(ctx, 12), dpOf(ctx, 18), dpOf(ctx, 12))
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        setOnClickListener { onClick() }
        onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus -> Anim.focus(v, hasFocus) }
        layoutParams = params(bottom = 8)
    }

    private fun params(top: Int = 0, bottom: Int): LinearLayout.LayoutParams {
        val ctx = rootView.context
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpOf(ctx, top)
            bottomMargin = dpOf(ctx, bottom)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
