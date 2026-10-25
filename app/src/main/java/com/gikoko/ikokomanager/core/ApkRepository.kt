package com.gikoko.ikokomanager.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.io.File

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val apkFile: String,
    val splitApkFiles: List<String>,
    val isSystemApp: Boolean,
    val sizeBytes: Long
)

data class ApkInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int?,
    val targetSdk: Int,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>
)

class ApkRepository(private val context: Context) {

    /** All installed apps. Extracting their APKs needs no root. */
    fun getInstalledApps(includeSystemApps: Boolean): List<InstalledApp> {
        val pm = context.packageManager
        val result = mutableListOf<InstalledApp>()
        for (pkg in pm.getInstalledPackages(PackageManager.GET_META_DATA)) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem && !includeSystemApps) continue
            val splits = pkg.splitNames?.indices
                ?.mapNotNull { i -> pkg.splitSourceDirs?.getOrNull(i) }
                .orEmpty()
            result.add(
                InstalledApp(
                    packageName = pkg.packageName,
                    appName = appInfo.loadLabel(pm).toString(),
                    versionName = pkg.versionName ?: "",
                    versionCode = pkg.versionCodeLong(),
                    apkFile = appInfo.sourceDir,
                    splitApkFiles = splits,
                    isSystemApp = isSystem,
                    sizeBytes = File(appInfo.sourceDir).length() +
                            splits.sumOf { File(it).length() }
                )
            )
        }
        return result.sortedBy { it.appName.lowercase() }
    }

    /** Manifest-level info of a standalone APK file (copied to cache first). */
    fun getApkInfo(apkUri: Uri): ApkInfo? {
        val tmp = File(context.cacheDir, "apk_inspect_${System.currentTimeMillis()}")
        try {
            context.contentResolver.openInputStream(apkUri)?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            } ?: return null
            val pm = context.packageManager
            val flags = PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
            val pkg: PackageInfo = pm.getPackageArchiveInfo(tmp.absolutePath, flags) ?: return null
            val appInfo = pkg.applicationInfo ?: return null
            return ApkInfo(
                packageName = pkg.packageName,
                versionName = pkg.versionName ?: "",
                versionCode = pkg.versionCodeLong(),
                minSdk = appInfo.minSdkVersion,
                targetSdk = appInfo.targetSdkVersion,
                permissions = pkg.requestedPermissions.orEmpty().toList(),
                activities = pkg.activities.orEmpty().map { it.name },
                services = pkg.services.orEmpty().map { it.name },
                receivers = pkg.receivers.orEmpty().map { it.name },
                providers = pkg.providers.orEmpty().map { it.name }
            )
        } finally {
            tmp.delete()
        }
    }

    /** Copy a SAF document into the cache so file-path-based tools can read it. */
    fun tempCopy(uri: Uri, name: String): File {
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val f = File(context.cacheDir, "pick_${System.currentTimeMillis()}_$safe")
        context.contentResolver.openInputStream(uri)?.use { input ->
            f.outputStream().use { input.copyTo(it) }
        }
        return f
    }

    private fun PackageInfo.versionCodeLong(): Long =
        if (Build.VERSION.SDK_INT >= 28) longVersionCode
        else @Suppress("DEPRECATION") versionCode.toLong()
}
