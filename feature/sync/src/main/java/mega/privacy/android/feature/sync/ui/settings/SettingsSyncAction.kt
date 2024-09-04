package mega.privacy.android.feature.sync.ui.settings

import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.feature.sync.ui.model.SyncOption

internal sealed class SettingsSyncAction {

    data class SyncOptionSelected(val option: SyncOption) : SettingsSyncAction()

    data object ClearDebrisClicked : SettingsSyncAction()

    data class SyncFrequencySelected(val frequency: SyncFrequency) : SettingsSyncAction()

    data object SnackbarShown : SettingsSyncAction()
}