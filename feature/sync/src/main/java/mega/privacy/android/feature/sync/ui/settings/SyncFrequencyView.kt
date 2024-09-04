package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem

@Composable
internal fun SyncFrequencyView(
    currentSyncFrequency: SyncFrequency,
    syncFrequencyClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GenericTwoLineListItem(
        modifier = modifier.testTag(SETTINGS_SYNC_SYNC_FREQUENCY_VIEW),
        title = "Sync frequency (QA option)",
        subtitle = frequencyToString(currentSyncFrequency),
        showEntireSubtitle = true,
        onItemClicked = syncFrequencyClicked,
    )
}

private fun frequencyToString(syncFrequency: SyncFrequency): String {
    return when (syncFrequency) {
        SyncFrequency.EVERY_15_MINUTES -> "15 minutes"
        SyncFrequency.EVERY_30_MINUTES -> "30 minutes"
        SyncFrequency.EVERY_45_MINUTES -> "45 minutes"
        SyncFrequency.EVERY_HOUR -> "1 hour"
    }
}

private const val SETTINGS_SYNC_SYNC_FREQUENCY_VIEW = "SETTINGS_SYNC_FREQUENCY_VIEW"