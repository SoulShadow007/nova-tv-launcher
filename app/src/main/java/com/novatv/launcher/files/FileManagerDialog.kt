package com.novatv.launcher.files

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.novatv.launcher.R
import com.novatv.launcher.ui.Prefs
import com.novatv.launcher.ui.themedInflater
import java.io.File
import java.util.Locale

/**
 * Built-in file manager. Modes:
 *  - IMAGE / VIDEO: picker that returns the chosen file path via a fragment result;
 *  - BROWSE: free navigation, files open with system viewers via FileProvider.
 */
class FileManagerDialog : DialogFragment() {

    private var mode: Int = MODE_BROWSE
    private var currentDir: File = rootDir()

    private lateinit var adapter: FilesAdapter
    private lateinit var pathText: TextView
    private lateinit var hintText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.NovaDialog)
        mode = arguments?.getInt(ARG_MODE, MODE_BROWSE) ?: MODE_BROWSE
        if (savedInstanceState != null) {
            val path = savedInstanceState.getString(ARG_DIR)
            if (!path.isNullOrEmpty()) currentDir = File(path)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = themedInflater(inflater).inflate(R.layout.dialog_files, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pathText = view.findViewById(R.id.files_path)
        hintText = view.findViewById(R.id.files_hint)
        val recycler = view.findViewById<RecyclerView>(R.id.files_recycler)
        view.findViewById<TextView>(R.id.files_up).setOnClickListener { navigateUp() }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = FilesAdapter(onPick = ::handleFile)
        recycler.adapter = adapter

        if (hasStoragePermission()) {
            reload()
        } else {
            requestPermissions(requiredPermissions(), REQ_PERM)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_DIR, currentDir.absolutePath)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != REQ_PERM) return
        if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            reload()
        } else {
            Toast.makeText(requireContext(), R.string.files_permission_needed, Toast.LENGTH_LONG).show()
            dismiss()
        }
    }

    private fun reload() {
        val rows = currentDir.listFiles()
            ?.map { FilesAdapter.FileRow(it, it.isDirectory) }
            ?.filter { it.isDirectory || matchesMode(it.file) }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.file.name.lowercase(Locale.ROOT) }))
            ?: emptyList()
        pathText.text = currentDir.absolutePath
        hintText.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        adapter.submit(rows)
    }

    private fun matchesMode(f: File): Boolean {
        val ext = f.extension.lowercase(Locale.ROOT)
        return when (mode) {
            MODE_IMAGE -> ext in FilesAdapter.IMAGE_EXT
            MODE_VIDEO -> ext in FilesAdapter.VIDEO_EXT
            else -> ext in FilesAdapter.IMAGE_EXT || ext in FilesAdapter.VIDEO_EXT
        }
    }

    private fun navigateUp() {
        val parent = currentDir.parentFile ?: return
        currentDir = parent
        reload()
    }

    private fun handleFile(f: File) {
        if (f.isDirectory) {
            currentDir = f
            reload()
            return
        }
        if (mode == MODE_BROWSE) {
            openExternally(f)
        } else {
            parentFragmentManager.setFragmentResult(
                Prefs.REQ_FILE_PICK,
                bundleOf(
                    Prefs.EXTRA_PATH to f.absolutePath,
                    Prefs.EXTRA_TYPE to if (mode == MODE_VIDEO) "video" else "image"
                )
            )
            dismiss()
        }
    }

    private fun openExternally(f: File) {
        val ext = f.extension.lowercase(Locale.ROOT)
        val mime = if (ext in FilesAdapter.VIDEO_EXT) "video/*" else "image/*"
        val uri: Uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            f
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(requireContext(), R.string.files_no_handler, Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasStoragePermission(): Boolean {
        val pm = requireContext().packageManager
        return requiredPermissions().any {
            pm.checkPermission(it, requireContext().packageName) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    companion object {
        const val MODE_BROWSE = 0
        const val MODE_IMAGE = 1
        const val MODE_VIDEO = 2

        private const val ARG_MODE = "mode"
        private const val ARG_DIR = "dir"
        private const val REQ_PERM = 41

        fun newInstance(mode: Int): FileManagerDialog = FileManagerDialog().apply {
            arguments = bundleOf(ARG_MODE to mode)
        }

        @Suppress("DEPRECATION")
        fun rootDir(): File = Environment.getExternalStorageDirectory()
    }
}
