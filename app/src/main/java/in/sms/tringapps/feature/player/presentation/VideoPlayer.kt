package `in`.sms.tringapps.feature.player.presentation

import android.graphics.Color as AndroidColor
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.media.toMediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val POSITION_SAVE_INTERVAL_MS = 1_000L

/** Duration once the manifest is parsed; 0 while unknown (or for live streams). */
private fun Player.knownDurationMs(): Long = duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L

/**
 * Media3 ExoPlayer bound to a [PlayerView]. Owns the player lifecycle:
 * pauses on stop, resumes on start, releases on dispose, and reports the position.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    video: Video,
    startPositionMs: Long,
    onBack: () -> Unit,
    onProgress: (positionMs: Long, durationMs: Long, isFinal: Boolean) -> Unit,
    onPlaybackError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mediaItem = remember(video.mediaId) { video.toMediaItem() } ?: return
    val context = LocalContext.current
    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnError by rememberUpdatedState(onPlaybackError)
    var controlsVisible by remember { mutableStateOf(true) }

    val player = remember(video.mediaId) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(mediaItem, startPositionMs)
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) = currentOnError()
        }
        player.addListener(listener)
        onDispose {
            currentOnProgress(player.currentPosition, player.knownDurationMs(), true)
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            delay(POSITION_SAVE_INTERVAL_MS)
            currentOnProgress(player.currentPosition, player.knownDurationMs(), false)
        }
    }

    // Declared after the DisposableEffect so it is disposed first (before release).
    val resumeOnStart = remember { booleanArrayOf(true) }
    LifecycleStartEffect(player) {
        if (resumeOnStart[0]) player.play()
        onStopOrDispose {
            resumeOnStart[0] = player.playWhenReady
            player.pause()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag(PlayerTestTags.VIDEO),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    setBackgroundColor(AndroidColor.BLACK)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    keepScreenOn = true
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlsVisible = visibility == View.VISIBLE
                        },
                    )
                    this.player = player
                }
            },
            update = { it.player = player },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            PlayerTopBar(title = video.title, onBack = onBack)
        }
    }
}
