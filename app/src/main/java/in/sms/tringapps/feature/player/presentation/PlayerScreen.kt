package `in`.sms.tringapps.feature.player.presentation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.component.GradientButton
import `in`.sms.tringapps.core.designsystem.theme.AccentViolet
import `in`.sms.tringapps.core.designsystem.theme.TextSecondary

object PlayerTestTags {
    const val LOADING = "player_loading"
    const val ERROR = "player_error"
    const val RETRY = "player_retry"
    const val BACK = "player_back"
    const val VIDEO = "player_video"
}

@Composable
fun PlayerRoute(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ImmersiveMode()
    PlayerScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
    ) { video, startPositionMs ->
        VideoPlayer(
            video = video,
            startPositionMs = startPositionMs,
            onBack = onBack,
            onProgress = viewModel::onProgress,
            onPlaybackError = viewModel::onPlaybackError,
        )
    }
}

/**
 * Stateless player screen. The actual video surface is a slot so the screen can be
 * tested on the JVM without a real decoder.
 */
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    videoContent: @Composable (video: Video, startPositionMs: Long) -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (state) {
            is PlayerUiState.Ready -> videoContent(state.video, state.startPositionMs)
            PlayerUiState.Loading -> {
                CircularProgressIndicator(
                    color = AccentViolet,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag(PlayerTestTags.LOADING),
                )
                PlayerTopBar(title = null, onBack = onBack)
            }
            is PlayerUiState.Error -> {
                PlayerErrorContent(state.reason, onRetry, Modifier.align(Alignment.Center))
                PlayerTopBar(title = null, onBack = onBack)
            }
        }
    }
}

@Composable
internal fun PlayerTopBar(title: String?, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
            .safeDrawingPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag(PlayerTestTags.BACK)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.player_back),
                tint = Color.White,
            )
        }
        if (title != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlayerErrorContent(reason: PlayerError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(32.dp)
            .testTag(PlayerTestTags.ERROR),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                when (reason) {
                    PlayerError.NETWORK -> R.string.player_error_network
                    PlayerError.NOT_FOUND -> R.string.player_error_not_found
                    PlayerError.NOT_PLAYABLE -> R.string.player_error_not_playable
                    PlayerError.PLAYBACK -> R.string.player_error_playback
                },
            ),
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (reason == PlayerError.NETWORK || reason == PlayerError.PLAYBACK) {
            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = stringResource(R.string.retry),
                icon = Icons.Filled.Refresh,
                onClick = onRetry,
                modifier = Modifier.testTag(PlayerTestTags.RETRY),
            )
        }
    }
}

/** Hides system bars while the player is on screen; swipe from the edge reveals them. */
@Composable
private fun ImmersiveMode() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
