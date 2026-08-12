package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.SyncPowerOption
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun SyncPowerOptionView(
    syncPowerOption: SyncPowerOption,
    modifier: Modifier = Modifier,
    syncPowerOptionsClicked: () -> Unit,
) {
    FlexibleLineListItem(
        modifier = modifier.testTag(SETTINGS_SYNC_POWER_OPTIONS_VIEW),
        title = stringResource(sharedR.string.settings_sync_battery_usage_title),
        subtitle = when (syncPowerOption) {
            SyncPowerOption.SyncAlways -> stringResource(sharedR.string.settings_sync_power_always_title)
            SyncPowerOption.SyncOnlyWhenCharging -> stringResource(sharedR.string.settings_sync_battery_sync_only_when_charging_title)
        },
        enableClick = true,
        onClickListener = syncPowerOptionsClicked,
    )
}

@Composable
@CombinedThemePreviews
private fun SyncPowerOptionsViewPreview(
    @PreviewParameter(BooleanProvider::class) syncOnlyWhenCharging: Boolean,
) {
    AndroidThemeForPreviews {
        SyncPowerOptionView(
            syncPowerOption = if (syncOnlyWhenCharging) SyncPowerOption.SyncOnlyWhenCharging else SyncPowerOption.SyncAlways,
            syncPowerOptionsClicked = {},
        )
    }
}

internal const val SETTINGS_SYNC_POWER_OPTIONS_VIEW = "SETTINGS_SYNC_POWER_OPTIONS_VIEW"
