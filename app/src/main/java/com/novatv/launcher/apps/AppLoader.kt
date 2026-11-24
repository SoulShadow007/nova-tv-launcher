package com.novatv.launcher.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.novatv.launcher.model.AppInfo

class AppLoader(private val context: Context) {

    /** All launchable activities on the device, except the launcher itself. */
    fun loadApps(): List<AppInfo> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, 0)
            .asSequence()
            .filter { it.activityInfo != null }
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { ri ->
                val ai = ri.activityInfo ?: return@mapNotNull null
                AppInfo(
                    label = ri.loadLabel(pm)?.toString().orEmpty(),
                    packageName = ai.packageName,
                    componentName = ComponentName(ai.packageName, ai.name),
                    icon = runCatching { ri.loadIcon(pm) }.getOrNull()
                )
            }
            .filter { it.label.isNotEmpty() }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
