package mega.privacy.android.app.di.settings.advanced

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.DefaultIsUseHttpsEnabled
import mega.privacy.android.app.domain.usecase.DefaultSetUseHttps
import mega.privacy.android.app.domain.usecase.IsUseHttpsEnabled
import mega.privacy.android.app.domain.usecase.SetUseHttps

/**
 * Settings advanced use cases module
 *
 * Provides use cases useds by the [mega.privacy.android.app.presentation.settings.advanced.SettingsAdvancedViewModel]
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class SettingsAdvancedUseCases {

    @Binds
    abstract fun bindIsUseHttpsEnabled(implementation: DefaultIsUseHttpsEnabled): IsUseHttpsEnabled

    @Binds
    abstract fun bindSetUseHttps(implementation: DefaultSetUseHttps): SetUseHttps

}