@file:OptIn(ExperimentalMaterial3Api::class)

package com.gikoko.ikokomanager.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gikoko.ikokomanager.core.ApkRepository
import com.gikoko.ikokomanager.core.InstalledApp
import com.gikoko.ikokomanager.core.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AppListScreen(
    modifier: Modifier = Modifier,
    apkRepository: ApkRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var includeSystem by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<InstalledApp?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(includeSystem) {
        loading = true
        apps = withContext(Dispatchers.IO) {
            apkRepository.getInstalledApps(includeSystem)
        }
        loading = false
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        val app = selected
        if (uri != null && app != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        File(app.apkFile).inputStream().use { it.copyTo(out) }
                    }
                }
                message = "APK saved"
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search apps") },
            singleLine = true
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            FilterChip(
                selected = includeSystem,
                onClick = { includeSystem = !includeSystem },
                label = { Text("Show system apps") }
            )
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        MessageBar(message) { message = null }

        val visible = apps.filter {
            it.appName.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
        }
        LazyColumn {
            items(visible, key = { it.packageName }) { app ->
                AppRow(app) { selected = app }
                HorizontalDivider()
            }
        }
    }

    selected?.let { app ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(app.appName) },
            text = {
                Column {
                    DetailLine("Package", app.packageName)
                    DetailLine("Version", "${app.versionName} (${app.versionCode})")
                    DetailLine("Type", if (app.isSystemApp) "System app" else "User app")
                    DetailLine("Size", formatBytes(app.sizeBytes))
                    DetailLine("APK path", app.apkFile)
                    if (app.splitApkFiles.isNotEmpty()) {
                        DetailLine("Splits", "${app.splitApkFiles.size} split APK(s)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { saveLauncher.launch("${app.appName}.apk") }) {
                    Text("Extract APK")
                }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun AppRow(app: InstalledApp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(app.appName, style = MaterialTheme.typography.bodyLarge)
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(formatBytes(app.sizeBytes), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Text(
        "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
