package com.example.galaxia.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxia.ui.theme.Cyan70
import com.example.galaxia.ui.theme.GalaxIATheme
import com.example.galaxia.ui.theme.Grey0
import com.example.galaxia.ui.theme.Grey60
import kotlinx.coroutines.delay

private const val SPLASH_DELAY_MS = 2000L

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {

    // Mantem a referencia mais recente do callback sem reiniciar a contagem
    val currentOnSplashFinished by rememberUpdatedState(onSplashFinished)

    LaunchedEffect(Unit) {
        delay(SPLASH_DELAY_MS)
        currentOnSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Grey0),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "GalaxIA",
                color = Cyan70,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "O universo, todos os dias, no seu bolso.",
                color = Grey60,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050609)
@Composable
private fun SplashScreenPreview() {

    GalaxIATheme {
        SplashScreen(onSplashFinished = { })
    }
}
