package com.gikoko.ikokomanager.core

import android.net.Uri
import androidx.documentfile.provider.DocumentFile

data class SafEntry(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val sizeBytes: Long
)

object FileRepository {

    fun list(dir: DocumentFile): List<SafEntry> =
        dir.listFiles()
            .map { f ->
                SafEntry(
                    name = f.name ?: "(unnamed)",
                    uri = f.uri,
                    isDirectory = f.isDirectory,
                    sizeBytes = f.length()
                )
            }
            .sortedWith(
                compareByDescending<SafEntry> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )

    fun isApk(name: String): Boolean = name.endsWith(".apk", ignoreCase = true)

    fun isZipLike(name: String): Boolean =
        listOf(".zip", ".jar", ".xapk", ".apkm", ".apks").any {
            name.endsWith(it, ignoreCase = true)
        }
}
