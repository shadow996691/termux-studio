package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.toColorInt
import com.example.ui.IdeApp
import com.example.ui.theme.TermuxCStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val darkBar = "#0B0E14".toColorInt()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(darkBar),
            navigationBarStyle = SystemBarStyle.dark(darkBar)
        )
        setContent {
            TermuxCStudioTheme {
                IdeApp()
            }
        }
    }
}
