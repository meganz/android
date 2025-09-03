package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.shimmerEffect
import mega.privacy.android.shared.resources.R

@Composable
internal fun SyncDebrisView(
    size: Long?,
    modifier: Modifier = Modifier,
    clearDebrisClicked: () -> Unit,
) {
    if (size == null) {
        SyncDebrisLoadingView(
            modifier = modifier.testTag(SETTINGS_SYNC_SYNC_DEBRIS_VIEW)
        )
    } else {
        GenericTwoLineListItem(
            modifier = modifier.testTag(SETTINGS_SYNC_SYNC_DEBRIS_VIEW),
            title = stringResource(R.string.settings_sync_clear_debris_item_title),
            subtitle = formatFileSize(size, LocalContext.current),
            showEntireSubtitle = true,
            onItemClicked = clearDebrisClicked,
        )
    }
}

@Composable
private fun SyncDebrisLoadingView(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            // Title shimmer
            Spacer(
                modifier = Modifier
                    .height(20.dp)
                    .width(200.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Subtitle shimmer
            Spacer(
                modifier = Modifier
                    .height(16.dp)
                    .width(80.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }
    }
}

@Composable
@CombinedThemePreviews
private fun SyncDebrisViewLoadedPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncDebrisView(
            size = 1024 * 1024 * 1024,
            clearDebrisClicked = {},
        )
    }
}

@Composable
@CombinedThemePreviews
private fun SyncDebrisViewLoadingPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncDebrisView(
            size = null,
            clearDebrisClicked = {},
        )
    }
}

private const val SETTINGS_SYNC_SYNC_DEBRIS_VIEW = "SETTINGS_SYNC_DEBRIS_VIEW"
