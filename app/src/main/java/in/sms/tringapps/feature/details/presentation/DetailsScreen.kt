package `in`.sms.tringapps.feature.details.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.component.GlassButton
import `in`.sms.tringapps.core.designsystem.component.GradientButton
import `in`.sms.tringapps.core.designsystem.component.MetaChip
import `in`.sms.tringapps.core.designsystem.component.RemoteImage
import `in`.sms.tringapps.core.designsystem.format.durationText
import `in`.sms.tringapps.core.designsystem.theme.AccentViolet
import `in`.sms.tringapps.core.designsystem.theme.BrandGradient
import `in`.sms.tringapps.core.designsystem.theme.Ink
import `in`.sms.tringapps.core.designsystem.theme.TextSecondary

object DetailsTestTags {
    const val ROOT = "details_root"
    const val LOADING = "details_loading"
    const val ERROR = "details_error"
    const val RETRY = "details_retry"
    const val BACK = "details_back"
    const val PLAY = "details_play"
    const val MY_LIST = "details_my_list"
    const val PROGRESS = "details_progress"
}

@Composable
fun DetailsRoute(
    onBack: () -> Unit,
    onPlay: (Video) -> Unit,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DetailsScreen(
        state = state,
        onBack = onBack,
        onPlay = onPlay,
        onToggleMyList = viewModel::toggleMyList,
        onRetry = viewModel::retry,
    )
}

@Composable
fun DetailsScreen(
    state: DetailsUiState,
    onBack: () -> Unit,
    onPlay: (Video) -> Unit,
    onToggleMyList: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Ink)
            .testTag(DetailsTestTags.ROOT),
    ) {
        when (state) {
            DetailsUiState.Loading -> CircularProgressIndicator(
                color = AccentViolet,
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag(DetailsTestTags.LOADING),
            )
            is DetailsUiState.Error -> DetailsError(state.kind, onRetry, Modifier.align(Alignment.Center))
            is DetailsUiState.Content -> DetailsContent(state, onPlay, onToggleMyList)
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                .testTag(DetailsTestTags.BACK),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
        }
    }
}

@Composable
private fun DetailsContent(
    state: DetailsUiState.Content,
    onPlay: (Video) -> Unit,
    onToggleMyList: () -> Unit,
) {
    val video = state.video
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 32.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f),
        ) {
            RemoteImage(
                url = video.backdropUrl,
                contentDescription = stringResource(R.string.cd_thumbnail, video.title),
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.5f),
                            0.3f to Color.Transparent,
                            0.7f to Color.Transparent,
                            1f to Ink,
                        ),
                    ),
            )
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(text = video.title, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                video.ageRating?.let { MetaChip(it, outlined = true) }
                durationText(video.durationSeconds)?.let { MetaChip(it, outlined = true) }
            }
            Spacer(Modifier.height(20.dp))
            if (video.isPlayable) {
                GradientButton(
                    text = stringResource(if (state.resumeProgress != null) R.string.details_resume else R.string.details_play),
                    icon = Icons.Filled.PlayArrow,
                    onClick = { onPlay(video) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DetailsTestTags.PLAY),
                )
                state.resumeProgress?.let { progress ->
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag(DetailsTestTags.PROGRESS),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .height(4.dp)
                                .background(BrandGradient, CircleShape),
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.player_error_not_playable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(targetState = state.isInMyList, label = "myList") { inList ->
                GlassButton(
                    text = stringResource(if (inList) R.string.my_list_remove else R.string.my_list_add),
                    icon = if (inList) Icons.Filled.Check else Icons.Filled.Add,
                    onClick = onToggleMyList,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DetailsTestTags.MY_LIST),
                )
            }
            if (video.description.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun DetailsError(kind: DetailsError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(32.dp)
            .testTag(DetailsTestTags.ERROR),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                if (kind == DetailsError.NETWORK) R.string.player_error_network else R.string.player_error_not_found,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (kind == DetailsError.NETWORK) {
            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = stringResource(R.string.retry),
                icon = Icons.Filled.Refresh,
                onClick = onRetry,
                modifier = Modifier.testTag(DetailsTestTags.RETRY),
            )
        }
    }
}
