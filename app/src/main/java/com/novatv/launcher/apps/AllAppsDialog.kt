package com.novatv.launcher.apps

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.novatv.launcher.R
import com.novatv.launcher.model.AppInfo
import com.novatv.launcher.themes.ThemeManager
import com.novatv.launcher.ui.Prefs
import com.novatv.launcher.ui.themedInflater

/** Fullscreen popup with every installed launchable app. */
class AllAppsDialog : DialogFragment() {

    private lateinit var favoritesStore: FavoritesStore
    private lateinit var adapter: AppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.NovaDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = themedInflater(inflater)
        .inflate(R.layout.dialog_all_apps, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favoritesStore = FavoritesStore(requireContext())
        val recycler = view.findViewById<RecyclerView>(R.id.all_apps_recycler)
        val countText = view.findViewById<TextView>(R.id.all_apps_count)

        val density = resources.displayMetrics.density
        val span = (resources.displayMetrics.widthPixels / (150 * density))
            .toInt().coerceIn(4, 8)
        recycler.layoutManager = GridLayoutManager(requireContext(), span)

        adapter = AppsAdapter(AppsAdapter.Mode.ALL_APPS, onOpen = ::openApp)
        adapter.isFavorite = { favoritesStore.isFavorite(it) }
        adapter.onToggleFavorite = ::toggleFavorite
        recycler.adapter = adapter

        val apps = AppLoader(requireContext()).loadApps()
        adapter.submit(apps)
        countText.text = getString(R.string.all_apps_count, apps.size)
    }

    private fun openApp(app: AppInfo) {
        val intent = Intent()
            .setComponent(app.componentName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(requireContext(), R.string.app_launch_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFavorite(app: AppInfo) {
        val added = favoritesStore.toggle(app)
        adapter.notifyDataSetChanged()
        val res = if (added) R.string.added_to_favorites else R.string.removed_from_favorites
        Toast.makeText(requireContext(), getString(res, app.label), Toast.LENGTH_SHORT).show()
        parentFragmentManager.setFragmentResult(Prefs.REQ_APPS_CHANGED, bundleOf())
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
