package com.gikoko.ikokomanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gikoko.ikokomanager.ui.MainScreen
import com.gikoko.ikokomanager.ui.theme.IkokoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IkokoTheme {
                MainScreen()
            }
        }
    }
}
