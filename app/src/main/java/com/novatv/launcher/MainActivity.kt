package com.novatv.launcher

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.novatv.launcher.apps.AllAppsDialog
import com.novatv.launcher.apps.AppLoader
import com.novatv.launcher.apps.AppsAdapter
import com.novatv.launcher.apps.FavoritesStore
import com.novatv.launcher.files.FileManagerDialog
import com.novatv.launcher.files.FilesAdapter
import com.novatv.launcher.model.AppInfo
import com.novatv.launcher.settings.SettingsDialog
import com.novatv.launcher.settings.WallpaperDialog
import com.novatv.launcher.system.SystemInfoMonitor
import com.novatv.launcher.themes.ThemeManager
import com.novatv.launcher.ui.Anim
import com.novatv.launcher.ui.AnimationStyle
import com.novatv.launcher.ui.Prefs
import com.novatv.launcher.ui.PreviewData
import com.novatv.launcher.wallpaper.WallpaperController
import com.novatv.launcher.weather.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Home screen of the launcher. Everything is operated with a D-pad remote:
 * navigate favorites / bottom bar with arrows, open with OK, edit favorites
 * with MENU (or long OK), open dialogs from the bottom bar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var themeManager: ThemeManager
    private lateinit var favoritesStore: FavoritesStore
    private lateinit var appLoader: AppLoader
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var systemMonitor: SystemInfoMonitor
    private lateinit var wallpaperController: WallpaperController

    private lateinit var favoritesAdapter: AppsAdapter

    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var weatherText: TextView
    private lateinit var statusRam: TextView
    private lateinit var statusTemp: TextView
    private lateinit var statusNet: TextView
    private lateinit var statusVpn: TextView
    private lateinit var previewPanel: View
    private lateinit var previewAppName: TextView
    private lateinit var previewRecommended: TextView
    private lateinit var previewContinue: TextView
    private lateinit var previewViewers: TextView
    private lateinit var previewNew: TextView
    private lateinit var favoritesRecycler: RecyclerView
    private lateinit var favoritesHint: TextView
    private lateinit var btnAllApps: View
    private lateinit var btnWallpaper: View
    private lateinit var btnFiles: View
    private lateinit var btnSettings: View

    private var clockJob: Job? = null
    private var statusJob: Job? = null
    private var weatherJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs.get(this)
        themeManager = ThemeManager(prefs)
        setTheme(themeManager.current.themeRes)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        favoritesStore = FavoritesStore(this)
        appLoader = AppLoader(this)
        weatherRepository = WeatherRepository(this)
        systemMonitor = SystemInfoMonitor(this)
        wallpaperController = WallpaperController(this)

        bindViews()
        setupFavorites()
        setupBottomBar()
        registerFragmentListeners()

        wallpaperController.applyFromPrefs()
        updateClock()
        renderWeather(weatherRepository.cached())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        wallpaperController.resume()
    }

    override fun onResume() {
        super.onResume()
        startLoops()
        wallpaperController.resume()
    }

    override fun onPause() {
        super.onPause()
        stopLoops()
        wallpaperController.pause()
    }

    // ---- setup ---------------------------------------------------------

    private fun bindViews() {
        clockText = findViewById(R.id.clock_text)
        dateText = findViewById(R.id.date_text)
        weatherText = findViewById(R.id.weather_text)
        statusRam = findViewById(R.id.status_ram)
        statusTemp = findViewById(R.id.status_temp)
        statusNet = findViewById(R.id.status_net)
        statusVpn = findViewById(R.id.status_vpn)
        previewPanel = findViewById(R.id.preview_panel)
        previewAppName = findViewById(R.id.preview_app_name)
        previewRecommended = findViewById(R.id.preview_recommended)
        previewContinue = findViewById(R.id.preview_continue)
        previewViewers = findViewById(R.id.preview_viewers)
        previewNew = findViewById(R.id.preview_new)
        favoritesRecycler = findViewById(R.id.favorites_recycler)
        favoritesHint = findViewById(R.id.favorites_hint)
        btnAllApps = findViewById(R.id.btn_all_apps)
        btnWallpaper = findViewById(R.id.btn_wallpaper)
        btnFiles = findViewById(R.id.btn_files)
        btnSettings = findViewById(R.id.btn_settings)

        statusVpn.setOnClickListener {
            runCatching { startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }
        }
    }

    private fun setupFavorites() {
        favoritesAdapter = AppsAdapter(AppsAdapter.Mode.FAVORITES, onOpen = ::launchApp)
        favoritesAdapter.onToggleFavorite = { app ->
            favoritesStore.toggle(app)
            Toast.makeText(
                this,
                getString(R.string.removed_from_favorites, app.label),
                Toast.LENGTH_SHORT
            ).show()
            refreshFavorites()
        }
        favoritesAdapter.onItemFocus = { app -> showPreview(app) }
        favoritesRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        favoritesRecycler.adapter = favoritesAdapter
        refreshFavorites()
    }

    private fun setupBottomBar() {
        btnAllApps.setOnClickListener { showDialog(AllAppsDialog()) }
        btnWallpaper.setOnClickListener { showDialog(WallpaperDialog()) }
        btnFiles.setOnClickListener { showFileManager(FileManagerDialog.MODE_BROWSE) }
        btnSettings.setOnClickListener { showDialog(SettingsDialog()) }
        listOf(btnAllApps, btnWallpaper, btnFiles, btnSettings).forEach { btn ->
            btn.setOnFocusChangeListener { v, hasFocus -> Anim.focus(v, hasFocus) }
        }
    }

    private fun registerFragmentListeners() {
        supportFragmentManager.setFragmentResultListener(Prefs.REQ_FILE_PICK, this) { _, bundle ->
            val path = bundle.getString(Prefs.EXTRA_PATH)
            val type = bundle.getString(Prefs.EXTRA_TYPE)
            if (!path.isNullOrEmpty()) {
                if (type == "video") {
                    wallpaperController.setVideo(path)
                    Toast.makeText(this, R.string.wallpaper_video_set, Toast.LENGTH_SHORT).show()
                } else {
                    wallpaperController.setImage(path)
                    Toast.makeText(this, R.string.wallpaper_image_set, Toast.LENGTH_SHORT).show()
                }
            }
        }
        supportFragmentManager.setFragmentResultListener(Prefs.REQ_APPS_CHANGED, this) { _, _ ->
            refreshFavorites()
        }
        supportFragmentManager.setFragmentResultListener(Prefs.REQ_CITY_CHANGED, this) { _, _ ->
            updateWeatherNow()
        }
    }

    // ---- favorites -----------------------------------------------------

    fun refreshFavorites() {
        val favorites = appLoader.loadApps().filter { favoritesStore.isFavorite(it) }
        favoritesAdapter.submit(favorites)
        favoritesHint.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
        if (favorites.isEmpty()) btnAllApps.requestFocus()
    }

    private fun launchApp(app: AppInfo) {
        val intent = Intent()
            .setComponent(app.componentName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, R.string.app_launch_error, Toast.LENGTH_SHORT).show()
        }
    }

    // ---- preview -------------------------------------------------------

    private fun showPreview(app: AppInfo?) {
        if (app == null) {
            if (previewPanel.visibility == View.VISIBLE) Anim.disappear(previewPanel)
            return
        }
        val data = PreviewData.build(this, app)
        previewAppName.text = app.label
        previewRecommended.text = data.recommended
        previewContinue.text = data.continueWatching
        previewViewers.text = data.viewersValue
        previewNew.text = data.newItems
        Anim.appear(previewPanel, currentAnimStyle())
    }

    private fun currentAnimStyle(): AnimationStyle =
        AnimationStyle.byKey(prefs.getString(Prefs.KEY_ANIM, null))

    // ---- dialogs -------------------------------------------------------

    fun showFileManager(mode: Int) {
        showDialog(FileManagerDialog.newInstance(mode))
    }

    fun resetWallpaper() {
        wallpaperController.reset()
        Toast.makeText(this, R.string.wallpaper_reset_done, Toast.LENGTH_SHORT).show()
    }

    private fun showDialog(dialog: DialogFragment) {
        dialog.show(supportFragmentManager, null)
    }

    // ---- periodic updates ----------------------------------------------

    private fun startLoops() {
        stopLoops()
        clockJob = lifecycleScope.launch {
            while (isActive) {
                updateClock()
                delay(1000)
            }
        }
        statusJob = lifecycleScope.launch {
            while (isActive) {
                updateStatus()
                delay(2000)
            }
        }
        weatherJob = lifecycleScope.launch {
            updateWeatherNow()
            while (isActive) {
                delay(30 * 60 * 1000L)
                updateWeatherNow()
            }
        }
    }

    private fun stopLoops() {
        clockJob?.cancel()
        clockJob = null
        statusJob?.cancel()
        statusJob = null
        weatherJob?.cancel()
        weatherJob = null
    }

    private fun updateClock() {
        val now = Date()
        clockText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        dateText.text = SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(now)
    }

    private fun updateStatus() {
        val s = systemMonitor.sample()
        statusRam.text =
            getString(R.string.status_ram, s.ramUsedPct, FilesAdapter.formatSize(s.ramAvail))
        statusTemp.text = if (s.tempC != null) {
            getString(R.string.status_temp, s.tempC)
        } else {
            getString(R.string.status_temp_na)
        }
        statusNet.text = if (s.downBps != null) {
            getString(R.string.status_net, formatSpeed(s.downBps))
        } else {
            getString(R.string.status_net_na)
        }
        statusVpn.text = if (s.vpnActive) {
            getString(R.string.status_vpn_on)
        } else {
            getString(R.string.status_vpn_off)
        }
        statusVpn.setTextColor(
            themeColor(if (s.vpnActive) R.attr.novaAccent else R.attr.novaTextSecondary)
        )
    }

    private fun formatSpeed(bps: Float): String = when {
        bps >= 1_000_000f -> String.format(Locale.US, "%.1f МБ/с", bps / 1_000_000f)
        bps >= 1_000f -> String.format(Locale.US, "%d КБ/с", (bps / 1000f).roundToInt())
        else -> "0 КБ/с"
    }

    // ---- weather -------------------------------------------------------

    private fun updateWeatherNow() {
        lifecycleScope.launch {
            renderWeather(weatherRepository.fetchCurrent())
        }
    }

    private fun renderWeather(w: WeatherRepository.WeatherData?) {
        if (w == null) {
            weatherText.text = getString(R.string.weather_unavailable)
            return
        }
        weatherText.text = getString(
            R.string.weather_format,
            w.emoji,
            w.temperatureC,
            getString(w.descriptionRes),
            w.windMs
        )
    }

    // ---- misc ----------------------------------------------------------

    private fun themeColor(attr: Int): Int {
        val tv = TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return tv.data
    }
}
