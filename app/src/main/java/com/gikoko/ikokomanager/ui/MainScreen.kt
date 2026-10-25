package com.gikoko.ikokomanager.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gikoko.ikokomanager.R
import com.gikoko.ikokomanager.core.ApkRepository
import com.gikoko.ikokomanager.core.ApkSigner
import com.gikoko.ikokomanager.core.DexEditorService

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val apkRepository = remember { ApkRepository(context) }
    val dexEditorService = remember { DexEditorService(context) }
    val signer = remember { ApkSigner() }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ikoko_logo),
                        contentDescription = "I KO KO Manager",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "I KO KO Manager",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Offline APK / DEX modding tools",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Files") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text("Apps") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    label = { Text("Dex") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    label = { Text("Tools") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> FileBrowserScreen(
                modifier = Modifier.padding(padding),
                apkRepository = apkRepository,
                dexEditorService = dexEditorService,
                onSessionStarted = { selectedTab = 2 }
            )
            1 -> AppListScreen(
                modifier = Modifier.padding(padding),
                apkRepository = apkRepository
            )
            2 -> DexEditorScreen(
                modifier = Modifier.padding(padding),
                dexEditorService = dexEditorService
            )
            3 -> ToolsScreen(
                modifier = Modifier.padding(padding),
                apkRepository = apkRepository,
                signer = signer
            )
        }
    }
}
