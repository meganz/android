package mega.privacy.android.app.di.settings.advanced

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.DefaultSetUseHttps
import mega.privacy.android.domain.usecase.IsUseHttpsEnabled
import mega.privacy.android.domain.usecase.SetUseHttps

/**
 * Settings advanced use cases module
 *
 * Provides use cases useds by the [mega.privacy.android.app.presentation.settings.advanced.SettingsAdvancedViewModel]
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class SettingsAdvancedUseCases {

    @Binds
    abstract fun bindSetUseHttps(implementation: DefaultSetUseHttps): SetUseHttps

    companion object {
        @Provides
        fun provideIsUseHttpsEnabled(settingsRepository: SettingsRepository): IsUseHttpsEnabled =
            IsUseHttpsEnabled(settingsRepository::isUseHttpsPreferenceEnabled)
    }

}