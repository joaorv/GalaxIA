package com.example.galaxia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.galaxia.ui.feed.FeedScreen
import com.example.galaxia.ui.splash.SplashScreen
import com.example.galaxia.ui.theme.GalaxIATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GalaxIATheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    // Temporario: substituir por um NavHost quando a navegacao entrar
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    } else {
                        FeedScreen()
                    }
                }
            }
        }
    }
}
