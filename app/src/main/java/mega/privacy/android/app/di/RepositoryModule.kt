package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.repository.*
import mega.privacy.android.app.domain.repository.*
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
    abstract fun bindChatRepository(repository: DefaultChatRepository): ChatRepository

    @Binds
    abstract fun bindFilesRepository(implementation: MegaFilesRepository): FilesRepository

    @Binds
    abstract fun bindFavouritesRepository(repository: DefaultFavouritesRepository): FavouritesRepository

    @Singleton
    @Binds
    abstract fun bindLoggingRepository(repository: TimberLoggingRepository): LoggingRepository

    @Binds
    abstract fun bindAlbumsRepository(repository: DefaultAlbumsRepository): AlbumsRepository

    @Binds
    abstract fun bindGlobalUpdatesRepository(repository: DefaultGlobalUpdatesRepository): GlobalUpdatesRepository
}