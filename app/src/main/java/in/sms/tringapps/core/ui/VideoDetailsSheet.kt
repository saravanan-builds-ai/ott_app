package `in`.sms.tringapps.core.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.component.GlassButton
import `in`.sms.tringapps.core.designsystem.component.GradientButton
import `in`.sms.tringapps.core.designsystem.component.MetaChip
import `in`.sms.tringapps.core.designsystem.component.RemoteImage
import `in`.sms.tringapps.core.designsystem.format.durationText
import `in`.sms.tringapps.core.designsystem.theme.InkRaised
import `in`.sms.tringapps.core.designsystem.theme.TextSecondary

/**
 * "More info" sheet: backdrop, title, metadata, Play, optional My List toggle and the
 * full description. Pass [onToggleMyList] = null to hide the toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailsSheet(
    video: Video,
    onPlay: (Video) -> Unit,
    onDismiss: () -> Unit,
    isInMyList: Boolean = false,
    onToggleMyList: ((Video) -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = InkRaised,
    ) {
        VideoDetailsContent(
            video = video,
            onPlay = onPlay,
            isInMyList = isInMyList,
            onToggleMyList = onToggleMyList,
        )
    }
}

@Composable
fun VideoDetailsContent(
    video: Video,
    onPlay: (Video) -> Unit,
    modifier: Modifier = Modifier,
    isInMyList: Boolean = false,
    onToggleMyList: ((Video) -> Unit)? = null,
) {
    Column(
        modifier
            .testTag(CatalogTestTags.DETAILS)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.large),
        ) {
            RemoteImage(
                url = video.backdropUrl,
                contentDescription = stringResource(R.string.cd_thumbnail, video.title),
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to InkRaised)),
            )
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text(text = video.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                video.ageRating?.let { MetaChip(it, outlined = true) }
                durationText(video.durationSeconds)?.let { MetaChip(it, outlined = true) }
            }
            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = stringResource(R.string.details_play),
                icon = Icons.Filled.PlayArrow,
                onClick = { onPlay(video) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CatalogTestTags.DETAILS_PLAY),
            )
            if (onToggleMyList != null) {
                Spacer(Modifier.height(12.dp))
                AnimatedContent(targetState = isInMyList, label = "myList") { inList ->
                    GlassButton(
                        text = stringResource(if (inList) R.string.my_list_remove else R.string.my_list_add),
                        icon = if (inList) Icons.Filled.Check else Icons.Filled.Add,
                        onClick = { onToggleMyList(video) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(CatalogTestTags.DETAILS_MY_LIST),
                    )
                }
            }
            if (video.description.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}
