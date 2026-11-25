package com.example.galaxia.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxia.R
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxIATheme
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaGray
import kotlinx.coroutines.delay

private const val SPLASH_DELAY_MS = 2200L

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Mantem a referencia mais recente do callback sem reiniciar a contagem
    val currentOnSplashFinished by rememberUpdatedState(onSplashFinished)

    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animação de surgimento suave da logo
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        delay(SPLASH_DELAY_MS - 800)
        currentOnSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GalaxiaBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_galaxia_logo),
                contentDescription = "Logo GalaxIA",
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "GalaxIA",
                color = GalaxiaCyan,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "O universo, todos os dias, no seu bolso.",
                color = GalaxiaGray,
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
        SplashScreen(onSplashFinished = {})
    }
}
