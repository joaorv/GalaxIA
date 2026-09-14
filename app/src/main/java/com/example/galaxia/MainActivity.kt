package com.example.galaxia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.galaxia.ui.feed.FeedScreen
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
                    FeedScreen()
                }
            }
        }
    }
}