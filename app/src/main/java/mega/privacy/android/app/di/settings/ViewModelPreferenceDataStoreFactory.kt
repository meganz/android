package mega.privacy.android.app.di.settings

import dagger.assisted.AssistedFactory
import mega.privacy.android.app.presentation.settings.SettingsViewModel
import mega.privacy.android.app.presentation.settings.ViewModelPreferenceDataStore

@AssistedFactory
interface ViewModelPreferenceDataStoreFactory {
    fun create(settingsViewModel: SettingsViewModel): ViewModelPreferenceDataStore
}