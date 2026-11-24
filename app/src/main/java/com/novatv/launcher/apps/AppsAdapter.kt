package com.novatv.launcher.apps

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.novatv.launcher.R
import com.novatv.launcher.model.AppInfo
import com.novatv.launcher.ui.Anim

/**
 * Grid/row adapter used both for the favorites bar on the home screen
 * and for the "All apps" dialog. Handles D-pad focus scaling, opening
 * (OK button) and adding/removing favorites (MENU button or long click).
 */
class AppsAdapter(
    private val mode: Mode,
    private val onOpen: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppsAdapter.Holder>() {

    enum class Mode { FAVORITES, ALL_APPS }

    private val items = mutableListOf<AppInfo>()

    var isFavorite: (AppInfo) -> Boolean = { false }
    var onToggleFavorite: (AppInfo) -> Unit = {}
    var onItemFocus: (AppInfo?) -> Unit = {}

    fun submit(list: List<AppInfo>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        items.getOrNull(position)?.let { holder.bind(it) }
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.app_icon)
        private val label: TextView = view.findViewById(R.id.app_label)
        private val badge: TextView = view.findViewById(R.id.fav_badge)

        fun bind(app: AppInfo) {
            label.text = app.label
            icon.setImageDrawable(app.icon)
            when (mode) {
                Mode.FAVORITES -> badge.visibility = View.GONE
                Mode.ALL_APPS -> {
                    badge.visibility = View.VISIBLE
                    badge.text = if (isFavorite(app)) "★" else "+"
                }
            }
            itemView.setOnClickListener { onOpen(app) }
            itemView.setOnLongClickListener {
                onToggleFavorite(app)
                true
            }
            itemView.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_DOWN) {
                    onToggleFavorite(app)
                    true
                } else {
                    false
                }
            }
            itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                Anim.focus(v, hasFocus)
                onItemFocus(if (hasFocus) app else null)
            }
        }
    }
}
