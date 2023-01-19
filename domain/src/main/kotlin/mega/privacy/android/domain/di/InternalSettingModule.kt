package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.setting.EnableFileVersionsOption

@Module
@DisableInstallInCheck
internal abstract class InternalSettingModule {
    companion object {
        @Provides
        fun provideEnableFileVersionsOption(repository: SettingsRepository) =
            EnableFileVersionsOption(repository::enableFileVersionsOption)
    }
}