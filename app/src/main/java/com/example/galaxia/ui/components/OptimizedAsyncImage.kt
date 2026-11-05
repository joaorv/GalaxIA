package com.example.galaxia.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Componente otimizado para carregamento de imagens em duas etapas:
 * 1. Carrega primeiro a imagem de menor tamanho / resolução (não HD).
 * 2. Assim que a imagem não HD for carregada, inicia o carregamento da imagem em alta definição (HD) em segundo plano,
 *    mantendo a versão leve visível até a HD concluir.
 */
@Composable
fun OptimizedAsyncImage(
    lowResUrl: String?,
    highResUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    val validLowRes = lowResUrl?.takeIf { it.isNotBlank() }
    val validHighRes = highResUrl?.takeIf { it.isNotBlank() && it != validLowRes }

    if (validLowRes == null && validHighRes == null) {
        Box(modifier = modifier.background(Color(0xFF161822)))
        return
    }

    if (validHighRes == null) {
        // Apenas uma imagem (não HD ou fallback)
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(validLowRes)
                .crossfade(true)
                .build()
        )
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier.background(Color(0xFF161822)),
            contentScale = contentScale
        )
    } else if (validLowRes == null) {
        // Apenas imagem HD disponível
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(validHighRes)
                .crossfade(true)
                .build()
        )
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier.background(Color(0xFF161822)),
            contentScale = contentScale
        )
    } else {
        // Duas etapas: carrega validLowRes primeiro e depois validHighRes
        var lowResState by remember(validLowRes) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
        var highResState by remember(validHighRes) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

        val lowResPainter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(validLowRes)
                .crossfade(true)
                .build(),
            onState = { state -> lowResState = state }
        )

        val isLowResFinished = lowResState is AsyncImagePainter.State.Success || lowResState is AsyncImagePainter.State.Error

        // Inicia o pedido da imagem HD assim que a imagem leve finalizar (sucesso ou erro)
        val highResModel = remember(context, validHighRes, isLowResFinished) {
            if (isLowResFinished) {
                ImageRequest.Builder(context)
                    .data(validHighRes)
                    .crossfade(true)
                    .build()
            } else {
                null
            }
        }

        val highResPainter = rememberAsyncImagePainter(
            model = highResModel,
            onState = { state -> highResState = state }
        )

        val isHighResSuccess = highResState is AsyncImagePainter.State.Success

        val painterToDraw: Painter = when {
            isHighResSuccess -> highResPainter
            lowResState is AsyncImagePainter.State.Success -> lowResPainter
            else -> lowResPainter
        }

        Image(
            painter = painterToDraw,
            contentDescription = contentDescription,
            modifier = modifier.background(Color(0xFF161822)),
            contentScale = contentScale
        )
    }
}
