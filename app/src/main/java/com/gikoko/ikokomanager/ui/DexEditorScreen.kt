@file:OptIn(ExperimentalMaterial3Api::class)

package com.gikoko.ikokomanager.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.core.content.FileProvider
import com.gikoko.ikokomanager.core.DexEditorService
import com.gikoko.ikokomanager.core.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DexEditorScreen(
    modifier: Modifier = Modifier,
    dexEditorService: DexEditorService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dexNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var signedApk by remember { mutableStateOf<File?>(null) }

    // smali browser state
    var browseDir by remember { mutableStateOf<File?>(null) }
    val browseStack = remember { mutableStateListOf<File>() }

    // text editor state
    var editingFile by remember { mutableStateOf<File?>(null) }
    var editingText by remember { mutableStateOf("") }
    var editingDirty by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            dexNames = if (dexEditorService.hasSession()) {
                dexEditorService.listDexEntries()
            } else emptyList()
            signedApk = dexEditorService.signedApkFile()
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        val signed = signedApk
        if (uri != null && signed != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        signed.inputStream().use { it.copyTo(out) }
                    }
                }
                message = "Signed APK saved"
            }
        }
    }

    fun install(file: File) {
        val uri = FileProvider.getUriForFile(
            context, context.packageName + ".fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            message = "No installer found: ${e.message}"
        }
    }

    // ---------------- smali text editor ----------------
    editingFile?.let { file ->
        Column(modifier.fillMaxSize().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (editingDirty) {
                        scope.launch {
                            withContext(Dispatchers.IO) { file.writeText(editingText) }
                            editingDirty = false
                            editingFile = null
                            message = "Saved"
                        }
                    } else {
                        editingFile = null
                    }
                }) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                Text(
                    file.name,
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { file.writeText(editingText) }
                        editingDirty = false
                        message = "Saved"
                    }
                }) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Save")
                }
            }
            OutlinedTextField(
                value = editingText,
                onValueChange = {
                    editingText = it
                    editingDirty = true
                },
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        return
    }

    // ---------------- smali file browser ----------------
    browseDir?.let { dir ->
        var filter by remember(dir) { mutableStateOf("") }
        Column(modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    browseDir = if (browseStack.isEmpty()) null
                    else browseStack.removeAt(browseStack.lastIndex)
                }) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                Text(
                    dir.name,
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
            }
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Filter files / classes") },
                singleLine = true
            )
            val files = remember(dir, filter) {
                dir.listFiles()
                    ?.sortedWith(
                        compareByDescending<File> { it.isDirectory }
                            .thenBy { it.name.lowercase() }
                    )
                    ?.filter { filter.isBlank() || it.name.contains(filter, ignoreCase = true) }
                    .orEmpty()
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(files, key = { it.absolutePath }) { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (f.isDirectory) {
                                    browseStack.add(dir)
                                    browseDir = f
                                } else if (f.name.endsWith(".smali")) {
                                    scope.launch {
                                        val text = withContext(Dispatchers.IO) { f.readText() }
                                        editingText = text
                                        editingDirty = false
                                        editingFile = f
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            (if (f.isDirectory) "[D] " else "") + f.name,
                            Modifier.weight(1f),
                            style = if (f.isDirectory) MaterialTheme.typography.bodyLarge
                            else MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                        if (!f.isDirectory) {
                            Text(
                                formatBytes(f.length()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
        return
    }

    // ---------------- session overview ----------------
    if (!dexEditorService.hasSession()) {
        EmptyState(
            title = "No modding session",
            message = "Open the Files tab, pick an APK and choose 'Edit DEX' " +
                    "to disassemble its dex code into smali and start editing."
        )
        MessageBar(message) { message = null }
        return
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    dexEditorService.sessionApkName(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    "${dexNames.size} dex file(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = {
                dexEditorService.clearSession()
                dexNames = emptyList()
                signedApk = null
            }) { Text("End") }
        }
        if (busy != null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                busy ?: "",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        MessageBar(message) { message = null }

        LazyColumn(Modifier.fillMaxSize()) {
            items(dexNames) { dexName ->
                DexRow(
                    dexName = dexName,
                    disassembled = dexEditorService.isDisassembled(dexName),
                    assembled = dexEditorService.isAssembled(dexName),
                    onDisassemble = {
                        busy = "Disassembling $dexName ..."
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    dexEditorService.disassemble(dexName)
                                }
                                busy = null
                                browseDir = dexEditorService.smaliDirFor(dexName)
                                browseStack.clear()
                            } catch (e: Exception) {
                                busy = null
                                message = e.message
                            }
                        }
                    },
                    onBrowse = {
                        browseDir = dexEditorService.smaliDirFor(dexName)
                        browseStack.clear()
                    },
                    onAssemble = {
                        busy = "Assembling $dexName ..."
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    dexEditorService.assemble(dexName)
                                }
                                busy = null
                                message = "$dexName rebuilt"
                            } catch (e: Exception) {
                                busy = null
                                message = e.message
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            busy = "Rebuilding and signing APK ..."
                            scope.launch {
                                try {
                                    val signed = withContext(Dispatchers.IO) {
                                        dexEditorService.buildAndSign()
                                    }
                                    signedApk = signed
                                    busy = null
                                    message = "Signed APK ready - save or install it"
                                } catch (e: Exception) {
                                    busy = null
                                    message = e.message
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = dexNames.any { dexEditorService.isAssembled(it) }
                    ) { Text("Build & Sign APK") }
                    Text(
                        "Builds every assembled dex into the APK, then re-signs it " +
                                "with your own key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    signedApk?.let { signed ->
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    saveLauncher.launch(
                                        "modded_${dexEditorService.sessionApkName()}"
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Save") }
                            OutlinedButton(
                                onClick = { install(signed) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Install") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DexRow(
    dexName: String,
    disassembled: Boolean,
    assembled: Boolean,
    onDisassemble: () -> Unit,
    onBrowse: () -> Unit,
    onAssemble: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dexName, Modifier.weight(1f))
            when {
                assembled -> AssistChip(onClick = {}, label = { Text("rebuilt") })
                disassembled -> AssistChip(onClick = {}, label = { Text("edited") })
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            if (!disassembled) {
                Button(onClick = onDisassemble) { Text("Disassemble") }
            } else {
                OutlinedButton(onClick = onBrowse) { Text("Browse smali") }
                Button(onClick = onAssemble) { Text("Assemble") }
            }
        }
    }
}
