package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun SyncBatterySaverView(
    pauseSyncOnBatterySaver: Boolean,
    modifier: Modifier = Modifier,
    pauseSyncOnBatterySaverChanged: (Boolean) -> Unit,
) {
    FlexibleLineListItem(
        modifier = modifier.testTag(SETTINGS_SYNC_BATTERY_SAVER_VIEW),
        title = stringResource(sharedR.string.settings_sync_battery_pause_on_battery_saver_title),
        trailingElement = {
            Toggle(
                modifier = Modifier.testTag(SETTINGS_SYNC_BATTERY_SAVER_SWITCH),
                isChecked = pauseSyncOnBatterySaver,
                onCheckedChange = pauseSyncOnBatterySaverChanged,
            )
        },
        enableClick = true,
        onClickListener = { pauseSyncOnBatterySaverChanged(!pauseSyncOnBatterySaver) },
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

internal const val SETTINGS_SYNC_BATTERY_SAVER_VIEW = "SETTINGS_SYNC_BATTERY_SAVER_VIEW"
internal const val SETTINGS_SYNC_BATTERY_SAVER_SWITCH = "SETTINGS_SYNC_BATTERY_SAVER_SWITCH"
