package com.novatv.launcher.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.VideoView
import androidx.lifecycle.lifecycleScope
import com.novatv.launcher.MainActivity
import com.novatv.launcher.R
import com.novatv.launcher.ui.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages static image and looping muted video wallpapers behind the UI.
 * Selection is persisted in SharedPreferences.
 */
class WallpaperController(private val activity: MainActivity) {

    private val prefs = Prefs.get(activity)
    private val videoView: VideoView = activity.findViewById(R.id.wallpaper_video)
    private val imageView: ImageView = activity.findViewById(R.id.wallpaper_image)

    fun applyFromPrefs() {
        val type = prefs.getString(Prefs.KEY_WALL_TYPE, null)
        val path = prefs.getString(Prefs.KEY_WALL_PATH, null)
        when (type) {
            TYPE_IMAGE -> if (!path.isNullOrEmpty() && File(path).exists()) loadImage(path) else reset()
            TYPE_VIDEO -> if (!path.isNullOrEmpty() && File(path).exists()) loadVideo(path) else reset()
            else -> reset()
        }
    }

    fun setImage(path: String) {
        prefs.edit()
            .putString(Prefs.KEY_WALL_TYPE, TYPE_IMAGE)
            .putString(Prefs.KEY_WALL_PATH, path)
            .apply()
        loadImage(path)
    }

    fun setVideo(path: String) {
        prefs.edit()
            .putString(Prefs.KEY_WALL_TYPE, TYPE_VIDEO)
            .putString(Prefs.KEY_WALL_PATH, path)
            .apply()
        loadVideo(path)
    }

    fun reset() {
        prefs.edit()
            .remove(Prefs.KEY_WALL_TYPE)
            .remove(Prefs.KEY_WALL_PATH)
            .apply()
        stopVideo()
        imageView.setImageResource(R.drawable.bg_wallpaper_default)
    }

    private fun loadImage(path: String) {
        stopVideo()
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val bmp = decodeSampled(path, 1920, 1080)
            withContext(Dispatchers.Main) {
                if (bmp != null) {
                    imageView.setImageBitmap(bmp)
                } else {
                    imageView.setImageResource(R.drawable.bg_wallpaper_default)
                }
            }
        }
    }

    private fun loadVideo(path: String) {
        stopVideo()
        imageView.setImageResource(R.drawable.bg_wallpaper_default)
        videoView.visibility = View.VISIBLE
        videoView.setVideoPath(path)
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
        }
        videoView.setOnCompletionListener { mp -> mp.start() }
        videoView.setOnErrorListener { _, _, _ ->
            reset()
            true
        }
        videoView.start()
    }

    private fun stopVideo() {
        videoView.stopPlayback()
        videoView.visibility = View.GONE
    }

    fun pause() {
        if (videoView.visibility == View.VISIBLE && videoView.isPlaying) videoView.pause()
    }

    fun resume() {
        if (videoView.visibility == View.VISIBLE && !videoView.isPlaying) videoView.start()
    }

    companion object {
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"

        fun decodeSampled(path: String, reqW: Int, reqH: Int): Bitmap? = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            var w = bounds.outWidth
            var h = bounds.outHeight
            while (w / 2 >= reqW || h / 2 >= reqH) {
                sample *= 2
                w /= 2
                h /= 2
            }
            BitmapFactory.decodeFile(
                path,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )
        }.getOrNull()
    }
}
