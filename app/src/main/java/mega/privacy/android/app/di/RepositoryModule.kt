package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.repository.DefaultAccountRepository
import mega.privacy.android.app.data.repository.DefaultAlbumsRepository
import mega.privacy.android.app.data.repository.DefaultAvatarRepository
import mega.privacy.android.app.data.repository.DefaultCameraUploadRepository
import mega.privacy.android.app.data.repository.DefaultContactsRepository
import mega.privacy.android.app.data.repository.DefaultFavouritesRepository
import mega.privacy.android.app.data.repository.DefaultFilesRepository
import mega.privacy.android.app.data.repository.DefaultGlobalStatesRepository
import mega.privacy.android.app.data.repository.DefaultImageRepository
import mega.privacy.android.app.data.repository.DefaultLoginRepository
import mega.privacy.android.app.data.repository.DefaultPhotosRepository
import mega.privacy.android.app.data.repository.DefaultSettingsRepository
import mega.privacy.android.app.data.repository.DefaultTransfersRepository
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.repository.GlobalStatesRepository
import mega.privacy.android.data.repository.TransfersRepository
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.AlbumsRepository
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.FileRepository
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.repository.TransferRepository
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
    abstract fun bindCameraUploadRepository(repository: DefaultCameraUploadRepository): CameraUploadRepository

    @Binds
    abstract fun bindFilesRepository(repository: DefaultFilesRepository): FilesRepository

    @Binds
    abstract fun bindDomainFilesRepository(repository: DefaultFilesRepository): FileRepository

    @Binds
    abstract fun bindFavouritesRepository(repository: DefaultFavouritesRepository): FavouritesRepository

    @Binds
    abstract fun bindAlbumsRepository(repository: DefaultAlbumsRepository): AlbumsRepository

    @Binds
    abstract fun bindGlobalUpdatesRepository(repository: DefaultGlobalStatesRepository): GlobalStatesRepository

    @Binds
    abstract fun bindGetImageRepository(repository: DefaultImageRepository): ImageRepository

    @Binds
    abstract fun bindContactsRepository(repository: DefaultContactsRepository): ContactsRepository

    @Binds
    abstract fun bindLoginRepository(repository: DefaultLoginRepository): LoginRepository

    @Binds
    @Singleton
    abstract fun bindAvatarRepository(repository: DefaultAvatarRepository): AvatarRepository

    @Binds
    abstract fun bindPhotosRepository(repository: DefaultPhotosRepository): PhotosRepository

    @Binds
    abstract fun bindTransfersRepository(repository: DefaultTransfersRepository): TransfersRepository

    /**
     * Bind domain transfers repository
     */
    @Binds
    abstract fun bindDomainTransfersRepository(repository: DefaultTransfersRepository): TransferRepository
}
