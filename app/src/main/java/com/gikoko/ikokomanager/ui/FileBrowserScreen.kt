@file:OptIn(ExperimentalMaterial3Api::class)

package com.gikoko.ikokomanager.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.gikoko.ikokomanager.core.ApkInfo
import com.gikoko.ikokomanager.core.ApkRepository
import com.gikoko.ikokomanager.core.DexEditorService
import com.gikoko.ikokomanager.core.FileRepository
import com.gikoko.ikokomanager.core.SafEntry
import com.gikoko.ikokomanager.core.ZipEntryInfo
import com.gikoko.ikokomanager.core.ZipService
import com.gikoko.ikokomanager.core.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FileBrowserScreen(
    modifier: Modifier = Modifier,
    apkRepository: ApkRepository,
    dexEditorService: DexEditorService,
    onSessionStarted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val zipService = remember { ZipService() }

    var currentDir by remember { mutableStateOf<DocumentFile?>(null) }
    var entries by remember { mutableStateOf<List<SafEntry>>(emptyList()) }
    val backStack = remember { mutableStateListOf<DocumentFile>() }

    var picked by remember { mutableStateOf<SafEntry?>(null) }
    var apkInfo by remember { mutableStateOf<ApkInfo?>(null) }
    var zipEntries by remember { mutableStateOf<List<ZipEntryInfo>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val treePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // read-only grant is still fine for browsing
            }
            backStack.clear()
            currentDir = DocumentFile.fromTreeUri(context, uri)
        }
    }

    val saveApk = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        val src = picked
        if (uri != null && src != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(src.uri)?.use { input ->
                        context.contentResolver.openOutputStream(uri)?.use { input.copyTo(it) }
                    }
                }
                message = "APK saved"
                picked = null
            }
        }
    }

    LaunchedEffect(currentDir) {
        entries = currentDir?.let { FileRepository.list(it) } ?: emptyList()
    }

    if (currentDir == null) {
        EmptyState(
            title = "No folder opened",
            message = "Pick any folder on your device to browse files - " +
                    "100% offline, no storage permission, no root.",
            actionLabel = "Open folder",
            onAction = { treePicker.launch(null) }
        )
        MessageBar(message) { message = null }
        return
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (backStack.isNotEmpty()) {
                TextButton(onClick = {
                    currentDir = backStack.removeAt(backStack.lastIndex)
                }) { Text("Up") }
            }
            Text(
                currentDir?.name ?: "Files",
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            TextButton(onClick = { treePicker.launch(null) }) { Text("Change") }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        MessageBar(message) { message = null }

        LazyColumn(Modifier.fillMaxSize()) {
            items(entries, key = { it.uri.toString() }) { entry ->
                SafRow(entry) {
                    if (entry.isDirectory) {
                        currentDir?.let { backStack.add(it) }
                        currentDir = DocumentFile.fromUri(context, entry.uri)
                    } else {
                        picked = entry
                        apkInfo = null
                        zipEntries = null
                        if (FileRepository.isApk(entry.name)) {
                            scope.launch {
                                apkInfo = withContext(Dispatchers.IO) {
                                    apkRepository.getApkInfo(entry.uri)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }

    // ---------- picked file actions ----------
    picked?.let { entry ->
        AlertDialog(
            onDismissRequest = { picked = null },
            title = { Text(entry.name, maxLines = 2) },
            text = {
                Column {
                    Text("Size: ${formatBytes(entry.sizeBytes)}")
                    if (apkInfo != null) {
                        val info = apkInfo!!
                        Spacer(Modifier.height(8.dp))
                        Text("Package: ${info.packageName}")
                        Text("Version: ${info.versionName} (${info.versionCode})")
                        Text("SDK: min ${info.minSdk ?: 0}, target ${info.targetSdk}")
                        Text("Permissions: ${info.permissions.size}")
                        Text("Activities: ${info.activities.size}")
                    }
                }
            },
            confirmButton = {
                if (FileRepository.isApk(entry.name)) {
                    TextButton(
                        onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        dexEditorService.startSession(entry.uri, entry.name)
                                    }
                                    busy = false
                                    picked = null
                                    onSessionStarted()
                                } catch (e: Exception) {
                                    busy = false
                                    message = "Failed: ${e.message}"
                                }
                            }
                        },
                        enabled = !busy
                    ) { Text(if (busy) "Working..." else "Edit DEX") }
                }
            },
            dismissButton = {
                Row {
                    if (FileRepository.isApk(entry.name)) {
                        TextButton(onClick = { saveApk.launch(entry.name) }) {
                            Text("Extract")
                        }
                    }
                    if (FileRepository.isApk(entry.name) || FileRepository.isZipLike(entry.name)) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    zipEntries = withContext(Dispatchers.IO) {
                                        try {
                                            zipService.listEntries(
                                                apkRepository.tempCopy(entry.uri, entry.name)
                                            )
                                        } catch (e: Exception) {
                                            message = "Not a zip: ${e.message}"
                                            null
                                        }
                                    }
                                }
                            }
                        ) { Text("Entries") }
                    }
                    TextButton(onClick = { picked = null }) { Text("Close") }
                }
            }
        )
    }

    // ---------- zip entries dialog ----------
    zipEntries?.let { list ->
        AlertDialog(
            onDismissRequest = { zipEntries = null },
            title = { Text("${list.size} entries") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(list, key = { it.name }) { e ->
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text(
                                e.name,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                maxLines = 2
                            )
                            Text(
                                if (e.isDirectory) "folder"
                                else "${formatBytes(e.sizeBytes)} " +
                                        "(${if (e.method == java.util.zip.ZipEntry.STORED) "stored" else "deflated"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { zipEntries = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SafRow(entry: SafEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (entry.isDirectory) "[D]" else iconFor(entry.name),
            modifier = Modifier.width(36.dp)
        )
        Text(
            entry.name,
            Modifier
                .weight(1f)
                .padding(end = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )
        if (!entry.isDirectory) {
            Text(
                formatBytes(entry.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun iconFor(name: String): String = when {
    FileRepository.isApk(name) -> "APK"
    FileRepository.isZipLike(name) -> "ZIP"
    else -> "·"
}
