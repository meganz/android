package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.android.core.ui.components.settings.SettingsToggleItem
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun SyncBatterySaverView(
    pauseSyncOnBatterySaver: Boolean,
    modifier: Modifier = Modifier,
    pauseSyncOnBatterySaverChanged: (Boolean) -> Unit,
) {
    SettingsToggleItem(
        modifier = modifier,
        key = SETTINGS_SYNC_BATTERY_SAVER_KEY,
        title = stringResource(sharedR.string.settings_sync_battery_pause_on_battery_saver_title),
        subtitle = null,
        checked = pauseSyncOnBatterySaver,
        onSettingsChanged = { _, newValue -> pauseSyncOnBatterySaverChanged(newValue) },
    )
}

@Composable
@CombinedThemePreviews
private fun SyncBatterySaverViewPreview(
    @PreviewParameter(BooleanProvider::class) pauseSyncOnBatterySaver: Boolean,
) {
    AndroidThemeForPreviews {
        SyncBatterySaverView(
            pauseSyncOnBatterySaver = pauseSyncOnBatterySaver,
            pauseSyncOnBatterySaverChanged = {},
        )
    }
}

internal const val SETTINGS_SYNC_BATTERY_SAVER_KEY = "sync_battery_saver"
