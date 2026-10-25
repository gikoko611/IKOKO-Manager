package com.gikoko.ikokomanager.core

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class ZipEntryInfo(
    val name: String,
    val sizeBytes: Long,
    val compressedBytes: Long,
    val isDirectory: Boolean,
    val method: Int
)

/**
 * Own zip engine built on java.util.zip. No code taken from any other app.
 *
 * Rebuilds preserve the original compression method per entry and keep
 * STORED entries (e.g. resources.arsc, native libs) aligned with a
 * zipalign-style padding extra field, so rebuilt APKs stay installable
 * on modern Android.
 */
class ZipService {

    fun listEntries(zip: File): List<ZipEntryInfo> {
        val result = mutableListOf<ZipEntryInfo>()
        ZipFile(zip).use { zf ->
            val e = zf.entries()
            while (e.hasMoreElements()) {
                val entry = e.nextElement()
                result.add(
                    ZipEntryInfo(
                        name = entry.name,
                        sizeBytes = entry.size,
                        compressedBytes = entry.compressedSize,
                        isDirectory = entry.isDirectory,
                        method = entry.method
                    )
                )
            }
        }
        return result.sortedBy { it.name }
    }

    /** All classes.dex / classes2.dex / ... entries of an APK. */
    fun listDexEntries(zip: File): List<String> =
        listEntries(zip)
            .filter { !it.isDirectory && Regex("^classes\\d*\\.dex$").matches(it.name) }
            .map { it.name }

    fun extractEntry(zip: File, entryName: String, out: File) {
        ZipFile(zip).use { zf ->
            val entry = zf.getEntry(entryName) ?: error("Entry not found: $entryName")
            out.parentFile?.mkdirs()
            zf.getInputStream(entry).use { input ->
                BufferedOutputStream(FileOutputStream(out)).use { input.copyTo(it) }
            }
        }
    }

    /**
     * Rebuild [zip] into [dest], replacing the entries named in [replacements]
     * with the given files.
     *
     * Old v1 JAR signature files are dropped because the rebuilt APK must be
     * re-signed anyway (v2/v3 blocks disappear automatically during rebuild).
     */
    fun rebuild(zip: File, dest: File, replacements: Map<String, File>) {
        ZipFile(zip).use { zf ->
            val counting = CountingOutputStream(BufferedOutputStream(FileOutputStream(dest)))
            ZipOutputStream(counting).use { zos ->
                zos.setMethod(ZipOutputStream.DEFLATED)
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val src = entries.nextElement()
                    if (!src.isDirectory && isOldSignatureFile(src.name)) continue

                    if (src.isDirectory) {
                        val dirEntry = ZipEntry(src.name)
                        dirEntry.method = ZipEntry.STORED
                        dirEntry.size = 0
                        dirEntry.compressedSize = 0
                        dirEntry.crc = 0
                        zos.putNextEntry(dirEntry)
                        zos.closeEntry()
                        continue
                    }

                    val replacement = replacements[src.name]
                    val outEntry = ZipEntry(src.name)
                    outEntry.time = src.time

                    if (replacement != null) {
                        outEntry.method = ZipEntry.DEFLATED
                        zos.putNextEntry(outEntry)
                        replacement.inputStream().use { it.copyTo(zos) }
                    } else if (src.method == ZipEntry.STORED) {
                        outEntry.method = ZipEntry.STORED
                        outEntry.size = src.size
                        outEntry.compressedSize = src.size
                        outEntry.crc = src.crc
                        outEntry.extra = alignmentPadding(counting.count, src)
                        zos.putNextEntry(outEntry)
                        zf.getInputStream(src).use { it.copyTo(zos) }
                    } else {
                        outEntry.method = ZipEntry.DEFLATED
                        zos.putNextEntry(outEntry)
                        zf.getInputStream(src).use { it.copyTo(zos) }
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun isOldSignatureFile(name: String): Boolean {
        if (!name.startsWith("META-INF/")) return false
        if (name == "META-INF/MANIFEST.MF") return true
        val file = name.substringAfterLast('/')
        return file.endsWith(".SF") || file.endsWith(".RSA") ||
                file.endsWith(".DSA") || file.endsWith(".EC")
    }

    /**
     * zipalign-style padding: a padding extra field (id 0xd935) sized so that
     * the entry data starts on a 4-byte boundary (16 bytes for .so files).
     */
    private fun alignmentPadding(streamPos: Long, entry: ZipEntry): ByteArray {
        val nameLen = entry.name.toByteArray(Charsets.UTF_8).size
        val dataStart = streamPos + 30 + nameLen // local file header + name
        val align = if (entry.name.endsWith(".so")) 16 else 4
        var pad = ((align - (dataStart % align)) % align).toInt()
        if (pad in 1..3) pad += align // an extra field costs at least 4 bytes
        if (pad == 0) return ByteArray(0)
        val extra = ByteArray(pad)
        extra[0] = 0x35.toByte()
        extra[1] = 0xd9.toByte()
        val size = pad - 4
        extra[2] = (size and 0xff).toByte()
        extra[3] = ((size shr 8) and 0xff).toByte()
        return extra
    }

    private class CountingOutputStream(out: OutputStream) : FilterOutputStream(out) {
        var count: Long = 0
            private set

        override fun write(b: Int) {
            out.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            count += len
        }
    }
}
