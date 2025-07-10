package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.feature.sync.ui.model.SyncPowerOption
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun SyncPowerOptionView(
    syncPowerOption: SyncPowerOption,
    modifier: Modifier = Modifier,
    syncPowerOptionsClicked: () -> Unit,
) {
    GenericTwoLineListItem(
        modifier = modifier.testTag(SETTINGS_SYNC_POWER_OPTIONS_VIEW),
        title = stringResource(sharedR.string.settings_sync_power_settings_title),
        subtitle = when (syncPowerOption) {
            SyncPowerOption.SyncAlways -> stringResource(sharedR.string.settings_sync_power_always_title)
            SyncPowerOption.SyncOnlyWhenCharging -> stringResource(sharedR.string.settings_sync_battery_sync_only_when_charging_title)
        },
        showEntireSubtitle = true,
        onItemClicked = syncPowerOptionsClicked,
    )
}

@Composable
@CombinedThemePreviews
private fun SyncPowerOptionsViewPreview(
    @PreviewParameter(BooleanProvider::class) syncOnlyWhenCharging: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncPowerOptionView(
            syncPowerOption = if (syncOnlyWhenCharging) SyncPowerOption.SyncOnlyWhenCharging else SyncPowerOption.SyncAlways,
            syncPowerOptionsClicked = {},
        )
    }
}

internal const val SETTINGS_SYNC_POWER_OPTIONS_VIEW = "SETTINGS_SYNC_POWER_OPTIONS_VIEW"
