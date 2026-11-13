package `in`.sms.tringapps.core.media

import android.graphics.Color as AndroidColor
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import `in`.sms.tringapps.catalog.domain.model.Video

/**
 * Silent, controller-less, SD-capped preview used behind the hero artwork.
 * Fades in only once the first frame is rendered, so the poster never flashes black.
 */
@OptIn(UnstableApi::class)
@Composable
fun MutedPreviewPlayer(
    video: Video,
    startPositionMs: Long,
    modifier: Modifier = Modifier,
) {
    val mediaItem = remember(video.mediaId) { video.toMediaItem() } ?: return
    val context = LocalContext.current
    var firstFrameRendered by remember(video.mediaId) { mutableStateOf(false) }

    val player = remember(video.mediaId) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                androidx.media3.common.AudioAttributes.DEFAULT,
                /* handleAudioFocus = */ false,
            )
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setMaxVideoSizeSd()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
                .build()
            setMediaItem(mediaItem, startPositionMs)
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LifecycleStartEffect(player) {
        player.play()
        onStopOrDispose { player.pause() }
    }

    val alpha by animateFloatAsState(
        targetValue = if (firstFrameRendered) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "previewAlpha",
    )

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                this.player = player
            }
        },
        update = { it.player = player },
        onRelease = { it.player = null },
        modifier = modifier.graphicsLayer { this.alpha = alpha },
    )
}
