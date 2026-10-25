package com.gikoko.ikokomanager.core

import android.content.Context
import android.net.Uri
import com.android.tools.smali.baksmali.Baksmali
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.smali.Smali
import com.android.tools.smali.smali.SmaliOptions
import java.io.File

/**
 * The DEX editing pipeline (the heart of the app):
 *
 *   pick APK -> copy into workspace
 *   -> extract classes.dex (or classes2.dex, ...)
 *   -> baksmali: dex  ->  smali code (readable .smali files)
 *   -> user edits .smali files in the built-in text editor
 *   -> smali:    smali ->  new dex
 *   -> rebuild the APK zip with the new dex (alignment-preserving)
 *   -> re-sign with the user's own key -> install
 *
 * Powered by the open-source smali/baksmali/dexlib2 libraries (BSD-3-Clause,
 * https://github.com/google/smali) - the same stack MT Manager-class tools use.
 * All glue code here is written from scratch for I KO KO Manager.
 */
class DexEditorService(private val context: Context) {

    private val sessionDir: File
        get() = File(context.cacheDir, SESSION_DIR)

    fun hasSession(): Boolean = File(sessionDir, ORIGINAL_APK).exists()

    fun sessionApkName(): String {
        val f = File(sessionDir, APK_NAME_FILE)
        return if (f.exists()) f.readText() else ""
    }

    /** Start a new session. Returns the dex entry names found in the APK. */
    fun startSession(apkUri: Uri, displayName: String): List<String> {
        sessionDir.deleteRecursively()
        sessionDir.mkdirs()
        val apkFile = File(sessionDir, ORIGINAL_APK)
        context.contentResolver.openInputStream(apkUri)?.use { input ->
            apkFile.outputStream().use { input.copyTo(it) }
        } ?: error("Cannot open the selected APK")
        File(sessionDir, APK_NAME_FILE).writeText(displayName)
        return listDexEntries()
    }

    fun listDexEntries(): List<String> =
        ZipService().listDexEntries(File(sessionDir, ORIGINAL_APK))

    fun smaliDirFor(dexName: String): File =
        File(sessionDir, "smali_" + dexName.removeSuffix(".dex"))

    fun rebuiltDexFor(dexName: String): File =
        File(sessionDir, "built_" + dexName)

    fun isDisassembled(dexName: String): Boolean = smaliDirFor(dexName).isDirectory

    fun isAssembled(dexName: String): Boolean = rebuiltDexFor(dexName).exists()

    /** Extract [dexName] and disassemble it to smali files. Returns the smali dir. */
    fun disassemble(dexName: String): File {
        val apkFile = File(sessionDir, ORIGINAL_APK)
        val dexFile = File(sessionDir, dexName)
        if (!dexFile.exists()) {
            ZipService().extractEntry(apkFile, dexName, dexFile)
        }

        val outDir = smaliDirFor(dexName)
        outDir.deleteRecursively()
        outDir.mkdirs()

        val dex = DexFileFactory.loadDexFile(dexFile, Opcodes.forApi(35))
        val options = BaksmaliOptions().apply {
            apiLevel = 35
            parameterRegisters = true
            debugInfo = true
            accessorComments = true
        }
        val ok = Baksmali.disassembleDexFile(dex, outDir, jobs(), options, null)
        if (!ok) error("baksmali failed for $dexName")
        return outDir
    }

    /** Assemble the edited smali files of [dexName] back into a dex. */
    fun assemble(dexName: String): File {
        val smaliDir = smaliDirFor(dexName)
        require(smaliDir.isDirectory) { "Disassemble $dexName first" }
        val outDex = rebuiltDexFor(dexName)
        val options = SmaliOptions().apply {
            apiLevel = 35
            outputDexFile = outDex.absolutePath
        }
        val ok = Smali.assemble(options, listOf(smaliDir.absolutePath))
        if (!ok) error("smali failed for $dexName - check the syntax of your edits")
        return outDex
    }

    /**
     * Rebuild the APK with every assembled dex, re-sign it with the user's
     * own key, and return the signed APK file (still inside the session dir).
     */
    fun buildAndSign(): File {
        val apkFile = File(sessionDir, ORIGINAL_APK)
        val replacements = mutableMapOf<String, File>()
        for (dexName in listDexEntries()) {
            if (isAssembled(dexName)) replacements[dexName] = rebuiltDexFor(dexName)
        }
        require(replacements.isNotEmpty()) { "Assemble at least one dex first" }

        val rebuilt = File(sessionDir, REBUILT_APK)
        if (rebuilt.exists()) rebuilt.delete()
        ZipService().rebuild(apkFile, rebuilt, replacements)

        val signed = File(sessionDir, SIGNED_APK)
        if (signed.exists()) signed.delete()
        ApkSigner().sign(rebuilt, signed)
        return signed
    }

    fun signedApkFile(): File? = File(sessionDir, SIGNED_APK).takeIf { it.exists() }

    fun clearSession() {
        sessionDir.deleteRecursively()
    }

    private fun jobs(): Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 8)

    companion object {
        private const val SESSION_DIR = "mod_session"
        private const val ORIGINAL_APK = "original.apk"
        private const val APK_NAME_FILE = "apk_name.txt"
        private const val REBUILT_APK = "rebuilt.apk"
        private const val SIGNED_APK = "signed.apk"
    }
}
