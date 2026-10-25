@file:OptIn(ExperimentalMaterial3Api::class)

package com.gikoko.ikokomanager.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.gikoko.ikokomanager.core.ApkRepository
import com.gikoko.ikokomanager.core.ApkSigner
import com.gikoko.ikokomanager.core.fileHashes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ToolsScreen(
    modifier: Modifier = Modifier,
    apkRepository: ApkRepository,
    signer: ApkSigner
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var signedFile by remember { mutableStateOf<File?>(null) }
    var hashes by remember { mutableStateOf<Map<String, String>?>(null) }

    val pickForSign = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    signedFile = withContext(Dispatchers.IO) {
                        val input = apkRepository.tempCopy(uri, "to_sign.apk")
                        val signed = File(context.cacheDir, "signed_${System.currentTimeMillis()}.apk")
                        signer.sign(input, signed)
                        signed
                    }
                    message = "Signing done - save the APK"
                } catch (e: Exception) {
                    message = "Sign failed: ${e.message}"
                }
                busy = false
            }
        }
    }

    val pickForHash = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    hashes = withContext(Dispatchers.IO) {
                        fileHashes(apkRepository.tempCopy(uri, "hash_input"))
                    }
                } catch (e: Exception) {
                    message = "Hash failed: ${e.message}"
                }
                busy = false
            }
        }
    }

    val saveSigned = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        val signed = signedFile
        if (uri != null && signed != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        signed.inputStream().use { it.copyTo(out) }
                    }
                }
                message = "Saved"
            }
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Tools", style = MaterialTheme.typography.headlineSmall)
        if (busy) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))
        MessageBar(message) { message = null }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Sign APK", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Re-sign any APK with your own key (v1 + v2 + v3). " +
                        "The key is generated inside the Android Keystore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        pickForSign.launch(arrayOf("application/vnd.android.package-archive"))
                    }) { Text("Pick APK") }
                    if (signedFile != null) {
                        OutlinedButton(onClick = { saveSigned.launch("signed.apk") }) {
                            Text("Save")
                        }
                        OutlinedButton(onClick = {
                            signedFile?.let { install(context, it) }
                        }) { Text("Install") }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Hash calculator", style = MaterialTheme.typography.titleMedium)
                Text(
                    "MD5 / SHA-1 / SHA-256 of any file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    pickForHash.launch(arrayOf("*/*"))
                }) { Text("Pick file") }
                hashes?.let { map ->
                    Spacer(Modifier.height(8.dp))
                    map.forEach { (algo, value) ->
                        Text("$algo: $value", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Coming in the next versions", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        AssistChip(onClick = {}, label = { Text("AndroidManifest (AXML) editor") })
        Spacer(Modifier.height(4.dp))
        AssistChip(onClick = {}, label = { Text("resources.arsc editor + translation mode") })
        Spacer(Modifier.height(4.dp))
        AssistChip(onClick = {}, label = { Text("XAPK / APKM converter") })
        Spacer(Modifier.height(24.dp))
    }
}

private fun install(context: android.content.Context, file: File) {
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
        // no installer available
    }
}
