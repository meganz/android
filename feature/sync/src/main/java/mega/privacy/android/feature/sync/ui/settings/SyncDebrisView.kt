package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.resources.R

@Composable
internal fun SyncDebrisView(
    size: Long,
    modifier: Modifier = Modifier,
    clearDebrisClicked: () -> Unit,
) {
    GenericTwoLineListItem(
        modifier = modifier.testTag(SETTINGS_SYNC_SYNC_OPTIONS_VIEW),
        title = stringResource(R.string.settings_sync_clear_debris_item_title),
        subtitle = formatFileSize(size, LocalContext.current),
        showEntireSubtitle = true,
        onItemClicked = clearDebrisClicked,
    )
}

@Composable
@CombinedThemePreviews
private fun SyncOptionsViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncDebrisView(
            size = 1024 * 1024 * 1024,
            clearDebrisClicked = {},
        )
    }
}

private const val SETTINGS_SYNC_SYNC_OPTIONS_VIEW = "SETTINGS_SYNC_DEBRIS_VIEW"