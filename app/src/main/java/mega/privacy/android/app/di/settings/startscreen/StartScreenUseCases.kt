package mega.privacy.android.app.di.settings.startscreen

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.SetStartScreenPreference

@Module
@InstallIn(ViewModelComponent::class)
abstract class StartScreenUseCases {

    companion object {

        /**
         * Provide monitor start screen preference
         *
         * @param repository
         */
        @Provides
        fun provideMonitorStartScreenPreference(repository: SettingsRepository): MonitorStartScreenPreference =
            MonitorStartScreenPreference(repository::monitorPreferredStartScreen)

        /**
         * Provide set start screen preference
         *
         * @param repository
         */
        @Provides
        fun provideSetStartScreenPreference(repository: SettingsRepository): SetStartScreenPreference =
            SetStartScreenPreference(repository::setPreferredStartScreen)

    }
}