package `in`.sms.tringapps.feature.home.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.sms.tringapps.R
import `in`.sms.tringapps.core.designsystem.theme.AccentPink
import `in`.sms.tringapps.core.designsystem.theme.TextSecondary

val RailPadding = PaddingValues(horizontal = 16.dp)

/** Rail title with an optional "See all ›" action. */
@Composable
fun RailHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
    seeAllTag: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(RailPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (onSeeAll != null) {
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onSeeAll)
                    .then(if (seeAllTag != null) Modifier.testTag(seeAllTag) else Modifier)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_see_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AccentPink,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
