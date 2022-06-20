package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.repository.DefaultAccountRepository
import mega.privacy.android.app.data.repository.DefaultAlbumsRepository
import mega.privacy.android.app.data.repository.DefaultCameraUploadRepository
import mega.privacy.android.app.data.repository.DefaultChatRepository
import mega.privacy.android.app.data.repository.DefaultFavouritesRepository
import mega.privacy.android.app.data.repository.DefaultFilesRepository
import mega.privacy.android.app.data.repository.DefaultGlobalStatesRepository
import mega.privacy.android.app.data.repository.DefaultNetworkRepository
import mega.privacy.android.app.data.repository.DefaultSettingsRepository
import mega.privacy.android.app.data.repository.DefaultThumbnailRepository
import mega.privacy.android.app.data.repository.TimberLoggingRepository
import mega.privacy.android.app.domain.repository.AccountRepository
import mega.privacy.android.app.domain.repository.AlbumsRepository
import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.app.domain.repository.ChatRepository
import mega.privacy.android.app.domain.repository.FavouritesRepository
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.repository.GlobalStatesRepository
import mega.privacy.android.app.domain.repository.LoggingRepository
import mega.privacy.android.app.domain.repository.NetworkRepository
import mega.privacy.android.app.domain.repository.SettingsRepository
import mega.privacy.android.app.domain.repository.ThumbnailRepository
import javax.inject.Singleton
import kotlin.contracts.ExperimentalContracts

/**
 * Repository module
 *
 * Provides all repository implementations
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @ExperimentalContracts
    @Singleton
    @Binds
    abstract fun bindSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository

    @ExperimentalContracts
    @Binds
    abstract fun bindAccountRepository(repository: DefaultAccountRepository): AccountRepository

    @Binds
    abstract fun bindNetworkRepository(repository: DefaultNetworkRepository): NetworkRepository

    @Binds
    abstract fun bindCameraUploadRepository(repository: DefaultCameraUploadRepository): CameraUploadRepository

    @Binds
    abstract fun bindChatRepository(repository: DefaultChatRepository): ChatRepository

    @Binds
    abstract fun bindFilesRepository(repository: DefaultFilesRepository): FilesRepository

    @Binds
    abstract fun bindFavouritesRepository(repository: DefaultFavouritesRepository): FavouritesRepository

    @Singleton
    @Binds
    abstract fun bindLoggingRepository(repository: TimberLoggingRepository): LoggingRepository

    @Binds
    abstract fun bindAlbumsRepository(repository: DefaultAlbumsRepository): AlbumsRepository

    @Binds
    abstract fun bindGlobalUpdatesRepository(repository: DefaultGlobalStatesRepository): GlobalStatesRepository

    @Binds
    abstract fun bindThumbnailRepository(repository: DefaultThumbnailRepository): ThumbnailRepository
}
