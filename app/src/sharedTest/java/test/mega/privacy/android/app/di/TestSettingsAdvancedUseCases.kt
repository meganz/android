package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.settings.advanced.SettingsAdvancedUseCases
import mega.privacy.android.app.domain.usecase.IsUseHttpsEnabled
import mega.privacy.android.app.domain.usecase.SetUseHttps
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [SettingsAdvancedUseCases::class],
    components = [ViewModelComponent::class]
)
object TestSettingsAdvancedUseCases {

    val isUseHttpsEnabled = mock<IsUseHttpsEnabled> { onBlocking { invoke() }.thenReturn(true)}
    val setUseHttps = mock<SetUseHttps>()

    @Provides
    fun bindIsUseHttpsEnabled(): IsUseHttpsEnabled = isUseHttpsEnabled

    @Provides
    fun bindSetUseHttps(): SetUseHttps = setUseHttps

}