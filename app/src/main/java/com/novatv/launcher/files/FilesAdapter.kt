package com.novatv.launcher.files

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.novatv.launcher.R
import com.novatv.launcher.ui.Anim
import java.io.File
import java.util.Locale

class FilesAdapter(
    private val onPick: (File) -> Unit
) : RecyclerView.Adapter<FilesAdapter.Holder>() {

    data class FileRow(val file: File, val isDirectory: Boolean)

    private val items = mutableListOf<FileRow>()

    fun submit(list: List<FileRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        items.getOrNull(position)?.let { holder.bind(it) }
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.file_icon)
        private val name: TextView = view.findViewById(R.id.file_name)
        private val subtitle: TextView = view.findViewById(R.id.file_subtitle)

        fun bind(row: FileRow) {
            name.text = row.file.name
            if (row.isDirectory) {
                icon.setImageResource(R.drawable.ic_folder)
                subtitle.setText(R.string.file_folder_subtitle)
            } else {
                val ext = row.file.extension.lowercase(Locale.ROOT)
                icon.setImageResource(
                    if (ext in VIDEO_EXT) R.drawable.ic_file_video else R.drawable.ic_file_image
                )
                subtitle.text = formatSize(row.file.length())
            }
            itemView.setOnClickListener { onPick(row.file) }
            itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                Anim.focus(v, hasFocus)
            }
        }
    }

    companion object {
        val VIDEO_EXT = setOf("mp4", "mkv", "webm", "avi", "mov", "ts", "3gp", "m4v")
        val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

        fun formatSize(bytes: Long): String = when {
            bytes >= 1L shl 30 -> String.format(Locale.US, "%.1f ГБ", bytes / 1073741824.0)
            bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f МБ", bytes / 1048576.0)
            bytes >= 1024 -> String.format(Locale.US, "%.0f КБ", bytes / 1024.0)
            else -> "$bytes Б"
        }
    }
}
