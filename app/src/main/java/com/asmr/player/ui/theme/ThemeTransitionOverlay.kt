package com.asmr.player.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.sqrt

data class ThemeTransitionRequest(
    val origin: Offset,
    val oldContentBitmap: ImageBitmap?,
    val oldBackgroundColor: Color,
    val token: Long
)

data class ThemeTransitionTriggerRequest(
    val origin: Offset,
    val targetPref: String
)

val LocalThemeTransitionTrigger = staticCompositionLocalOf<((ThemeTransitionTriggerRequest) -> Unit)?> { null }

@Composable
fun ThemeCircularRevealOverlay(
    request: ThemeTransitionRequest,
    onAnimationEnd: () -> Unit
) {
    val animationProgress = remember { Animatable(0f) }
    var overlaySize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(request) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
        onAnimationEnd()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { overlaySize = Size(it.width.toFloat(), it.height.toFloat()) }
        ) {
            if (overlaySize == Size.Zero) return@Canvas

            val maxRadius = sqrt(
                overlaySize.width * overlaySize.width + overlaySize.height * overlaySize.height
            )
            val radius = maxRadius * animationProgress.value

            val rectPath = Path().apply {
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, overlaySize.width, overlaySize.height))
            }
            val circlePath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        request.origin.x - radius,
                        request.origin.y - radius,
                        request.origin.x + radius,
                        request.origin.y + radius
                    )
                )
            }

            val outsidePath = Path().apply {
                op(rectPath, circlePath, PathOperation.Difference)
            }

            clipPath(outsidePath) {
                val bitmap = request.oldContentBitmap
                if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                    drawImage(
                        image = bitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
                        dstSize = androidx.compose.ui.unit.IntSize(
                            size.width.toInt(),
                            size.height.toInt()
                        )
                    )
                } else {
                    drawRect(request.oldBackgroundColor)
                }
            }
        }
    }
}
