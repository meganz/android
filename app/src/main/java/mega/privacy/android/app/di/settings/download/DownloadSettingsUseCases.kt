package mega.privacy.android.app.di.settings.download

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.GetStorageDownloadAskAlways
import mega.privacy.android.domain.usecase.GetStorageDownloadDefaultPath
import mega.privacy.android.domain.usecase.GetStorageDownloadLocation
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlways
import mega.privacy.android.domain.usecase.SetStorageDownloadLocation

/**
 * Dagger module for Use Cases in Download Settings Page
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class DownloadSettingsUseCases {
    companion object {
        /**
         * Binds Get Storage Default Download Location Path Use Case
         */
        @Provides
        fun providesGetDefaultDownloadLocationPath(repository: SettingsRepository): GetStorageDownloadDefaultPath =
            GetStorageDownloadDefaultPath {
                repository.buildDefaultDownloadDir().absolutePath
            }

        /**
         * Binds Get Storage Download Location Use Case
         */
        @Provides
        fun providesGetStorageDownloadLocationUseCase(repository: SettingsRepository): GetStorageDownloadLocation =
            GetStorageDownloadLocation(repository::getStorageDownloadLocation)

        /**
         * Binds Set Storage Download Location Use Case
         */
        @Provides
        fun providesSetStorageDownloadLocationUseCase(repository: SettingsRepository): SetStorageDownloadLocation =
            SetStorageDownloadLocation(repository::setStorageDownloadLocation)

        /**
         * Binds Get Storage Download Ask Always Use Case
         */
        @Provides
        fun providesGetStorageDownloadAskAlwaysUseCase(repository: SettingsRepository): GetStorageDownloadAskAlways =
            GetStorageDownloadAskAlways(repository::getStorageDownloadAskAlways)

        /**
         * Binds Set Storage Download Ask Always Use Case
         */
        @Provides
        fun providesSetStorageDownloadAskAlwaysUseCase(repository: SettingsRepository): SetStorageDownloadAskAlways =
            SetStorageDownloadAskAlways(repository::setStorageAskAlways)
    }
}