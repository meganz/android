package mega.privacy.android.feature.sync.ui.settings

import mega.privacy.android.feature.sync.ui.model.SyncConnectionType
import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.feature.sync.ui.model.SyncPowerOption

internal sealed class SettingsSyncAction {

    // Connection type Settings Actions
    data class SyncConnectionTypeSelected(val option: SyncConnectionType) : SettingsSyncAction()

    // Power Settings Actions
    data class SyncPowerOptionSelected(val option: SyncPowerOption) : SettingsSyncAction()

    // Other Actions
    data object ClearDebrisClicked : SettingsSyncAction()

    // Sync Frequency Settings Actions
    data class SyncFrequencySelected(val frequency: SyncFrequency) : SettingsSyncAction()

    // Snackbar Actions
    data object SnackbarShown : SettingsSyncAction()

    // Clear Sync Resolved Issues
    data object ClearSyncResolvedIssuesClicked : SettingsSyncAction()
}
