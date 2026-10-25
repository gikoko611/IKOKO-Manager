package com.gikoko.ikokomanager.core

import java.io.File
import java.security.MessageDigest

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}

/** Compute MD5 / SHA-1 / SHA-256 of a file. */
fun fileHashes(file: File): Map<String, String> {
    val result = mutableMapOf<String, String>()
    for (algo in listOf("MD5", "SHA-1", "SHA-256")) {
        val md = MessageDigest.getInstance(algo)
        file.inputStream().use { input ->
            val buf = ByteArray(1 shl 16)
            var read: Int
            while (input.read(buf).also { read = it } != -1) {
                md.update(buf, 0, read)
            }
        }
        result[algo] = md.digest().joinToString("") { "%02x".format(it) }
    }
    return result
}
