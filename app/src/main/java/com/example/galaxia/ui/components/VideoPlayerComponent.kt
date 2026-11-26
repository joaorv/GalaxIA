package com.example.galaxia.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxiaCardBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaWhite

/**
 * Componente unificado para reprodução otimizada de vídeos da NASA APOD (YouTube, Vimeo, MP4 Direto e Artigos da NASA).
 *
 * Estratégia de reprodução do YouTube:
 * 1. Utiliza a YouTube IFrame Player API oficial (https://developers.google.com/youtube/iframe_api_reference).
 * 2. Suporta URLs: youtube.com/watch, youtu.be, youtube.com/embed, youtube.com/shorts e youtube-nocookie.com.
 * 3. Passa a origem válida ('https://science.nasa.gov') no loadDataWithBaseURL e nos playerVars para evitar os erros 150/153.
 * 4. Captura os erros 100, 101, 150 e 153 do player para apresentar uma mensagem amigável e botão "Assistir no YouTube".
 */
@Composable
fun VideoMediaCard(
    mediaUrl: String,
    thumbnailUrl: String?,
    title: String?,
    permalinkUrl: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val uriHandler = LocalUriHandler.current

    var isPlaying by remember(mediaUrl) { mutableStateOf(false) }
    var isError by remember(mediaUrl) { mutableStateOf(false) }
    var errorMessage by remember(mediaUrl) { mutableStateOf<String?>(null) }

    val ytId = remember(mediaUrl) { ApodResponse.extractYouTubeVideoId(mediaUrl) }
    val vimeoId = remember(mediaUrl) { ApodResponse.extractVimeoVideoId(mediaUrl) }

    val nasaUrl = remember(permalinkUrl, mediaUrl) {
        permalinkUrl?.takeIf {
            it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://"))
        } ?: "https://science.nasa.gov/picture-of-the-day/"
    }

    val isDirectVideo = remember(mediaUrl) {
        val lower = mediaUrl.lowercase()
        lower.endsWith(".mp4") || lower.endsWith(".webm") ||
        lower.endsWith(".m3u8") || lower.endsWith(".m4v") ||
        lower.endsWith(".mov") || lower.contains("video.media.nasa.gov") ||
        lower.contains(".mp4?") || lower.contains(".webm?")
    }

    val isEmbeddableVideo = ytId != null || vimeoId != null || isDirectVideo

    val embedUrl = remember(mediaUrl, ytId, vimeoId) {
        when {
            ytId != null -> "https://www.youtube.com/embed/$ytId?autoplay=1&rel=0&enablejsapi=1"
            vimeoId != null -> "https://player.vimeo.com/video/$vimeoId?autoplay=1"
            else -> mediaUrl
        }
    }

    val badgeText = if (isEmbeddableVideo) "VÍDEO" else "ARTIGO NASA"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0E13)),
        contentAlignment = Alignment.Center
    ) {
        if (!isPlaying) {
            // === ESTADO 1: MINIATURA + OVERLAY + BOTÃO DE PLAY / ABRIR (Sem consumo de Player) ===
            Box(modifier = Modifier.fillMaxSize()) {
                if (!thumbnailUrl.isNullOrBlank()) {
                    OptimizedAsyncImage(
                        lowResUrl = thumbnailUrl,
                        highResUrl = null,
                        contentDescription = title ?: "Conteúdo da NASA",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(GalaxiaCardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title ?: "Conteúdo Astronômico",
                            color = GalaxiaWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Sombra gradiente
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                // Tag indicadora (VÍDEO ou ARTIGO NASA)
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isEmbeddableVideo) Icons.Default.Videocam else Icons.AutoMirrored.Filled.Article,
                            contentDescription = null,
                            tint = GalaxiaCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeText,
                            color = GalaxiaCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Botão de abrir no site da NASA (canto superior direito)
                IconButton(
                    onClick = {
                        try {
                            uriHandler.openUri(nasaUrl)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Abrir no site da NASA",
                        tint = GalaxiaWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Botão de Ação Centralizado (Play para vídeos incorporáveis, ou Abrir Artigo para páginas da NASA)
                Surface(
                    onClick = {
                        if (isEmbeddableVideo) {
                            isError = false
                            isPlaying = true
                        } else {
                            try {
                                uriHandler.openUri(nasaUrl)
                            } catch (_: Exception) {
                                isError = true
                                errorMessage = "Não foi possível abrir a publicação no site da NASA."
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp),
                    shape = CircleShape,
                    color = GalaxiaCyan.copy(alpha = 0.9f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isEmbeddableVideo) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = if (isEmbeddableVideo) "Reproduzir vídeo" else "Abrir no site da NASA",
                            tint = GalaxiaBackground,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        } else if (isError) {
            // === ESTADO 2: ERRO NATIVO OU RESTRIÇÃO DE REPRODUÇÃO ===
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GalaxiaCardBackground)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = null,
                    tint = GalaxiaCyan,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "Não foi possível carregar o vídeo internamente.",
                    color = GalaxiaWhite,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row {
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(nasaUrl)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GalaxiaCyan)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = GalaxiaBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Abrir no site da NASA",
                            color = GalaxiaBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (ytId != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri("https://www.youtube.com/watch?v=$ytId")
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202330))
                        ) {
                            Text("Abrir no YouTube", color = GalaxiaWhite, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { isPlaying = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202330))
                    ) {
                        Text("Fechar", color = GalaxiaWhite, fontSize = 12.sp)
                    }
                }
            }
        } else {
            // === ESTADO 3: PLAYER EM EXECUÇÃO ===
            Box(modifier = Modifier.fillMaxSize()) {
                if (ytId != null) {
                    YouTubeVideoPlayerContainer(
                        youtubeVideoId = ytId,
                        onError = { err ->
                            isError = true
                            errorMessage = err
                        }
                    )
                } else if (isDirectVideo) {
                    ExoPlayerContainer(
                        mediaUrl = mediaUrl,
                        onError = { err ->
                            isError = true
                            errorMessage = err
                        }
                    )
                } else {
                    WebViewVideoContainer(
                        embedUrl = embedUrl,
                        onError = { err ->
                            isError = true
                            errorMessage = err
                        }
                    )
                }

                // Botão de Fechar o Player (volta para a miniatura)
                IconButton(
                    onClick = { isPlaying = false },
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar reprodução de vídeo",
                        tint = GalaxiaWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Player oficial do YouTube utilizando a IFrame Player API.
 * Configura 'origin' e 'widget_referrer' válidos ('https://science.nasa.gov') para evitar o erro 150/153.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeVideoPlayerContainer(
    youtubeVideoId: String,
    onError: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                webViewInstance?.onPause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                webViewInstance?.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewInstance?.run {
                stopLoading()
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                destroy()
            }
            webViewInstance = null
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val urlStr = request?.url?.toString() ?: return false
                        if (urlStr.startsWith("galaxia://yt-error")) {
                            val codeStr = request.url.getQueryParameter("code")
                            val code = codeStr?.toIntOrNull() ?: -1
                            val msg = when (code) {
                                101, 150, 153 -> "A incorporação deste vídeo no aplicativo foi desativada pelo proprietário no YouTube."
                                100 -> "Este vídeo do YouTube não foi encontrado ou foi removido."
                                2 -> "Parâmetro inválido no vídeo do YouTube."
                                else -> "Não foi possível reproduzir este vídeo do YouTube no aplicativo."
                            }
                            onError(msg)
                            return true
                        }
                        return false
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onError("Não foi possível carregar o reprodutor do YouTube.")
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    @Suppress("DEPRECATION")
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        onError("Não foi possível carregar o reprodutor do YouTube.")
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        if (errorResponse?.statusCode in 400..599) {
                            onError("O servidor do YouTube respondeu com um erro ao tentar reproduzir.")
                        }
                    }
                }

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                            * { margin: 0; padding: 0; box-sizing: border-box; }
                            body, html { width: 100%; height: 100%; background-color: #050609; overflow: hidden; display: flex; justify-content: center; align-items: center; }
                            #player { width: 100%; height: 100%; }
                        </style>
                    </head>
                    <body>
                        <div id="player"></div>
                        <script>
                            var tag = document.createElement('script');
                            tag.src = "https://www.youtube.com/iframe_api";
                            var firstScriptTag = document.getElementsByTagName('script')[0];
                            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                            var player;
                            function onYouTubeIframeAPIReady() {
                                player = new YT.Player('player', {
                                    height: '100%',
                                    width: '100%',
                                    videoId: '$youtubeVideoId',
                                    playerVars: {
                                        'autoplay': 1,
                                        'playsinline': 1,
                                        'rel': 0,
                                        'enablejsapi': 1,
                                        'origin': 'https://science.nasa.gov',
                                        'widget_referrer': 'https://science.nasa.gov',
                                        'fs': 1
                                    },
                                    events: {
                                        'onReady': onPlayerReady,
                                        'onError': onPlayerError
                                    }
                                });
                            }

                            function onPlayerReady(event) {
                                event.target.playVideo();
                            }

                            function onPlayerError(event) {
                                window.location.href = "galaxia://yt-error?code=" + event.data;
                            }
                        </script>
                    </body>
                    </html>
                """.trimIndent()

                loadDataWithBaseURL("https://science.nasa.gov", html, "text/html", "utf-8", null)
                webViewInstance = this
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Player ExoPlayer leve e isolado para arquivos de vídeo diretos (.mp4, .m3u8).
 */
@Composable
private fun ExoPlayerContainer(
    mediaUrl: String,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(mediaUrl) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(mediaUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    onError("Falha na reprodução do arquivo de vídeo: ${error.localizedMessage ?: "Erro desconhecido"}")
                }
            })
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                exoPlayer.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Container WebView seguro e isolado para incorporação de vídeos do Vimeo e iFrames autorizados.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewVideoContainer(
    embedUrl: String,
    onError: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                webViewInstance?.onPause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                webViewInstance?.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewInstance?.run {
                stopLoading()
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                destroy()
            }
            webViewInstance = null
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onError("O servidor não permitiu a exibição incorporada deste conteúdo.")
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    @Suppress("DEPRECATION")
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        onError("O servidor não permitiu a exibição incorporada deste conteúdo.")
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        if (errorResponse?.statusCode in 400..599) {
                            onError("O servidor respondeu com um erro ao tentar reproduzir.")
                        }
                    }
                }

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                            * { margin: 0; padding: 0; box-sizing: border-box; }
                            body, html { width: 100%; height: 100%; background-color: #050609; overflow: hidden; display: flex; justify-content: center; align-items: center; }
                            iframe { width: 100%; height: 100%; border: 0; }
                        </style>
                    </head>
                    <body>
                        <iframe src="$embedUrl" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
                    </body>
                    </html>
                """.trimIndent()

                loadDataWithBaseURL("https://science.nasa.gov", html, "text/html", "utf-8", null)
                webViewInstance = this
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
