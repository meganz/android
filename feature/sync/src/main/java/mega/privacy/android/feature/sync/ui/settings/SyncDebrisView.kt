package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.settings.SettingsNavigationItem
import mega.android.core.ui.components.settings.SkeletonPreferenceItem
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.shared.resources.R

@Composable
internal fun SyncDebrisView(
    size: Long?,
    modifier: Modifier = Modifier,
    clearDebrisClicked: () -> Unit,
) {
    if (size == null) {
        SkeletonPreferenceItem(showFooter = true)
    } else {
        SettingsNavigationItem(
            modifier = modifier,
            key = SETTINGS_SYNC_DEBRIS_KEY,
            title = stringResource(R.string.settings_sync_clear_debris_item_title),
            subtitle = formatFileSize(size, LocalContext.current),
            onClicked = { clearDebrisClicked() },
        )
    }
}

@Composable
@CombinedThemePreviews
private fun SyncDebrisViewLoadedPreview() {
    AndroidThemeForPreviews {
        SyncDebrisView(
            size = 1024 * 1024 * 1024,
            clearDebrisClicked = {},
        )
    }
}

@Composable
@CombinedThemePreviews
private fun SyncDebrisViewLoadingPreview() {
    AndroidThemeForPreviews {
        SyncDebrisView(
            size = null,
            clearDebrisClicked = {},
        )
    }
}

internal const val SETTINGS_SYNC_DEBRIS_KEY = "sync_debris"
