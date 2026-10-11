package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.setting.EnableFileVersionsOption

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FileVersionsOptionModule {
    companion object {
        @Provides
        fun provideEnableFileVersionsOption(repository: SettingsRepository) =
            EnableFileVersionsOption(repository::enableFileVersionsOption)
    }
}
