package `in`.sms.tringapps.feature.home.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.component.GlassButton
import `in`.sms.tringapps.core.designsystem.component.GradientButton
import `in`.sms.tringapps.core.designsystem.component.MetaChip
import `in`.sms.tringapps.core.designsystem.component.RemoteImage
import `in`.sms.tringapps.core.designsystem.component.ShimmerBox
import `in`.sms.tringapps.core.designsystem.format.durationText
import `in`.sms.tringapps.core.designsystem.theme.AccentGold
import `in`.sms.tringapps.core.designsystem.theme.Ink
import `in`.sms.tringapps.core.designsystem.theme.TextSecondary
import `in`.sms.tringapps.core.media.MutedPreviewPlayer
import kotlinx.coroutines.delay

private const val AUTO_ADVANCE_MS = 7_000L
private const val PREVIEW_DELAY_MS = 2_500L
private const val PREVIEW_DURATION_MS = 25_000L
private const val ADVANCE_AFTER_PREVIEW_MS = 1_500L
private const val PREVIEW_MAX_START_MS = 120_000L

/**
 * Full-bleed featured carousel. After the page settles it fades in a muted preview of
 * the title (when [previewEnabled] and the hero is on screen), then advances.
 */
@Composable
fun HeroCarousel(
    videos: List<Video>,
    onPlay: (Video) -> Unit,
    onInfo: (Video) -> Unit,
    modifier: Modifier = Modifier,
    autoAdvance: Boolean = true,
    previewEnabled: Boolean = true,
    isVisible: Boolean = true,
) {
    val pagerState = rememberPagerState { videos.size }
    var previewing by remember { mutableStateOf(false) }
    var isStarted by remember { mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleStartEffect(Unit) {
        isStarted = true
        onStopOrDispose {
            isStarted = false
            previewing = false
        }
    }
    val heroActive = isVisible && isStarted

    LaunchedEffect(pagerState.settledPage, previewEnabled, heroActive, autoAdvance, videos.size) {
        previewing = false
        if (!heroActive) return@LaunchedEffect
        val canPreview = previewEnabled && videos.getOrNull(pagerState.settledPage)?.isPlayable == true
        if (canPreview) {
            delay(PREVIEW_DELAY_MS)
            previewing = true
            delay(PREVIEW_DURATION_MS)
            previewing = false
        }
        if (autoAdvance && videos.size > 1) {
            delay(if (canPreview) ADVANCE_AFTER_PREVIEW_MS else AUTO_ADVANCE_MS)
            pagerState.animateScrollToPage((pagerState.settledPage + 1) % videos.size)
        }
    }

    Box(modifier.testTag(HomeTestTags.HERO)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            HeroPage(
                video = videos[page],
                showPreview = previewing && page == pagerState.settledPage,
                onPlay = onPlay,
                onInfo = onInfo,
            )
        }
        if (videos.size > 1) {
            PagerDots(
                count = videos.size,
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun HeroPage(
    video: Video,
    showPreview: Boolean,
    onPlay: (Video) -> Unit,
    onInfo: (Video) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        RemoteImage(
            url = video.backdropUrl,
            contentDescription = stringResource(R.string.cd_thumbnail, video.title),
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedVisibility(
            visible = showPreview,
            enter = fadeIn(),
            exit = fadeOut(tween(600)),
            modifier = Modifier.fillMaxSize(),
        ) {
            MutedPreviewPlayer(
                video = video,
                startPositionMs = (video.durationSeconds * 1000 / 10).coerceAtMost(PREVIEW_MAX_START_MS),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Top scrim keeps the wordmark/status bar legible; bottom scrim blends into the page.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.6f),
                        0.22f to Color.Transparent,
                        0.5f to Color.Transparent,
                        0.82f to Ink.copy(alpha = 0.9f),
                        1f to Ink,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_featured).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AccentGold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = video.title,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                video.ageRating?.let { MetaChip(it, outlined = true) }
                durationText(video.durationSeconds)?.let { MetaChip(it, outlined = true) }
            }
            val heroDescription = remember(video.description) {
                video.description
                    .lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(" · ")
            }
            if (heroDescription.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = heroDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientButton(
                    text = stringResource(R.string.home_play),
                    icon = Icons.Filled.PlayArrow,
                    onClick = { onPlay(video) },
                    modifier = Modifier.testTag(HomeTestTags.HERO_PLAY),
                )
                GlassButton(
                    text = stringResource(R.string.home_more_info),
                    icon = Icons.Outlined.Info,
                    onClick = { onInfo(video) },
                    modifier = Modifier.testTag(HomeTestTags.HERO_INFO),
                )
            }
        }
    }
}

@Composable
private fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_hero_page, current + 1, count)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 18.dp else 6.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) Color.White else Color.White.copy(alpha = 0.35f),
                label = "dotColor",
            )
            Box(
                Modifier
                    .height(6.dp)
                    .width(width)
                    .background(color, CircleShape),
            )
        }
    }
}

@Composable
fun HeroPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier) {
        ShimmerBox(Modifier.fillMaxSize())
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(width = 220.dp, height = 28.dp)
                    .background(Ink.copy(alpha = 0.6f), MaterialTheme.shapes.small),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .size(width = 260.dp, height = 48.dp)
                    .background(Ink.copy(alpha = 0.6f), CircleShape),
            )
        }
    }
}
