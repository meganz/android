package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.settings.SettingsNavigationItem
import mega.privacy.android.feature.sync.ui.model.SyncFrequency

@Composable
internal fun SyncFrequencyView(
    currentSyncFrequency: SyncFrequency,
    syncFrequencyClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsNavigationItem(
        modifier = modifier,
        key = SETTINGS_SYNC_FREQUENCY_KEY,
        title = "Sync frequency (QA option)",
        subtitle = frequencyToString(currentSyncFrequency),
        onClicked = { syncFrequencyClicked() },
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

internal const val SETTINGS_SYNC_FREQUENCY_KEY = "sync_frequency"
