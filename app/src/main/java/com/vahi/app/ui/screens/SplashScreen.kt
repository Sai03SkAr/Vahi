package com.vahi.app.ui.screens

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/** Safety-net timeout in case the video fails to reach STATE_ENDED for any reason. */
private const val SPLASH_FALLBACK_TIMEOUT_MS = 6000L

@OptIn(UnstableApi::class)
@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    val context = LocalContext.current

    // Guards against double-navigation from the ended/error listener racing the fallback timeout.
    val hasNavigated = remember { mutableStateOf(false) }
    fun navigateOnce() {
        if (!hasNavigated.value) {
            hasNavigated.value = true
            onTimeout()
        }
    }

    // Create ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri("asset:///splash.mp4")
            setMediaItem(mediaItem)
            playWhenReady = true
            volume = 0f // No audio
            repeatMode = Player.REPEAT_MODE_OFF // Play once, no loop
            prepare()

            // Listen for playback end
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        navigateOnce()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    // Media failed to load/play — don't strand the user on the splash screen.
                    navigateOnce()
                }
            })
        }
    }

    // Fallback: if playback never reaches STATE_ENDED or errors out silently, advance anyway.
    LaunchedEffect(Unit) {
        delay(SPLASH_FALLBACK_TIMEOUT_MS)
        navigateOnce()
    }

    // Release player on dispose
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Animated background gradient
    val infiniteTransition = rememberInfiniteTransition(label = "bg_animation")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F6F4).copy(alpha = 1f - animatedOffset * 0.1f),
                        Color(0xFFFAF8F6),
                        Color(0xFFF5F3F1).copy(alpha = 1f - animatedOffset * 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Video player - centered with slight downward offset for visual balance
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // No controls
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
