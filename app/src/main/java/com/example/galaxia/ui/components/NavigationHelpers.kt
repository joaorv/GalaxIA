package com.example.galaxia.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.galaxia.ui.theme.GalaxiaCardBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Estado coordenado para recolher e reexibir suavemente o cabeçalho principal
 * durante a rolagem (estilo Twitter/X), sem sobrepor o conteúdo abaixo.
 */
@Stable
class CollapsibleHeaderState {
    var heightPx by mutableFloatStateOf(0f)
        private set

    private val offsetAnimatable = Animatable(0f)

    val offsetPx: Float
        get() = offsetAnimatable.value

    internal fun updateHeight(newHeightPx: Float) {
        if (newHeightPx > 0f && heightPx != newHeightPx) {
            heightPx = newHeightPx
        }
    }

    suspend fun expand() {
        if (offsetAnimatable.value != 0f) {
            offsetAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
    }

    fun createNestedScrollConnection(
        scope: CoroutineScope,
        canCollapse: () -> Boolean,
        isAtTop: () -> Boolean
    ): NestedScrollConnection = object : NestedScrollConnection {

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val h = heightPx
            if (h <= 0f) return Offset.Zero

            val current = offsetAnimatable.value
            val deltaY = available.y

            if (deltaY < 0f && canCollapse()) {
                val next = computeNextOffset(current, deltaY, h)
                val consumed = next - current
                if (consumed != 0f) {
                    scope.launch { offsetAnimatable.snapTo(next) }
                    return Offset(0f, consumed)
                }
            } else if (deltaY > 0f && current < 0f) {
                val next = computeNextOffset(current, deltaY, h)
                val consumed = next - current
                if (consumed != 0f) {
                    scope.launch { offsetAnimatable.snapTo(next) }
                    return Offset(0f, consumed)
                }
            }

            // Quando offset == 0 e o usuário puxa para baixo no topo, não consome nada
            // para que o PullToRefreshBox receba o gesto normalmente.
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val h = heightPx
            val current = offsetAnimatable.value
            if (h > 0f && current < 0f && current > -h) {
                val target = computeSnapTarget(current, h, isAtTop())
                offsetAnimatable.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            } else if (h > 0f && isAtTop() && current < 0f) {
                offsetAnimatable.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            }
            return Velocity.Zero
        }
    }

    companion object {
        fun computeNextOffset(currentOffset: Float, deltaY: Float, heightPx: Float): Float {
            val safeHeight = heightPx.coerceAtLeast(0f)
            return (currentOffset + deltaY).coerceIn(-safeHeight, 0f)
        }

        fun computeSnapTarget(currentOffset: Float, heightPx: Float, isAtTop: Boolean): Float {
            if (heightPx <= 0f || isAtTop || currentOffset > -heightPx * 0.5f) {
                return 0f
            }
            return -heightPx
        }
    }
}

@Composable
fun rememberCollapsibleHeaderState(): CollapsibleHeaderState = remember {
    CollapsibleHeaderState()
}

@Composable
fun CollapsibleHeaderContainer(
    state: CollapsibleHeaderState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val fullHeight = placeable.height
                state.updateHeight(fullHeight.toFloat())
                val offset = state.offsetPx.roundToInt()
                val visibleHeight = (fullHeight + offset).coerceIn(0, fullHeight)
                layout(placeable.width, visibleHeight) {
                    placeable.placeRelative(0, offset)
                }
            }
    ) {
        content()
    }
}

/**
 * Botão flutuante discreto na lateral direita para retorno animado ao topo da lista.
 */
@Composable
fun ScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.82f, animationSpec = tween(200)),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.82f, animationSpec = tween(180))
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = GalaxiaCardBackground.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, GalaxiaCyan.copy(alpha = 0.45f)),
            shadowElevation = 6.dp
        ) {
            androidx.compose.foundation.layout.Box(
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Voltar ao topo",
                    tint = GalaxiaCyan,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
