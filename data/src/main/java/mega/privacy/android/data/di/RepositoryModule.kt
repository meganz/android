package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.repository.DefaultAlbumsRepository
import mega.privacy.android.data.repository.DefaultAvatarRepository
import mega.privacy.android.data.repository.DefaultCameraUploadRepository
import mega.privacy.android.data.repository.DefaultChatRepository
import mega.privacy.android.data.repository.DefaultContactsRepository
import mega.privacy.android.data.repository.DefaultEnvironmentRepository
import mega.privacy.android.data.repository.DefaultFavouritesRepository
import mega.privacy.android.data.repository.DefaultFeatureFlagRepository
import mega.privacy.android.data.repository.DefaultFilesRepository
import mega.privacy.android.data.repository.DefaultGalleryFilesRepository
import mega.privacy.android.data.repository.DefaultGlobalStatesRepository
import mega.privacy.android.data.repository.DefaultImageRepository
import mega.privacy.android.data.repository.DefaultNetworkRepository
import mega.privacy.android.data.repository.DefaultNotificationsRepository
import mega.privacy.android.data.repository.DefaultPushesRepository
import mega.privacy.android.data.repository.DefaultRecentActionsRepository
import mega.privacy.android.data.repository.DefaultSettingsRepository
import mega.privacy.android.data.repository.DefaultSortOrderRepository
import mega.privacy.android.data.repository.DefaultStatisticsRepository
import mega.privacy.android.data.repository.DefaultSupportRepository
import mega.privacy.android.data.repository.DefaultTransfersRepository
import mega.privacy.android.data.repository.FilesRepository
import mega.privacy.android.data.repository.GlobalStatesRepository
import mega.privacy.android.data.repository.RecentActionsRepository
import mega.privacy.android.domain.repository.AlbumsRepository
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.FeatureFlagRepository
import mega.privacy.android.domain.repository.FileRepository
import mega.privacy.android.domain.repository.GalleryFilesRepository
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.repository.SortOrderRepository
import mega.privacy.android.domain.repository.StatisticsRepository
import mega.privacy.android.domain.repository.SupportRepository
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Singleton
import kotlin.contracts.ExperimentalContracts

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    abstract fun bindContactsRepository(repository: DefaultContactsRepository): ContactsRepository

    @Binds
    abstract fun bindNetworkRepository(repository: DefaultNetworkRepository): NetworkRepository

    @Binds
    abstract fun bindDeviceRepository(implementation: DefaultEnvironmentRepository): EnvironmentRepository

    @Binds
    @Singleton
    abstract fun bindFeatureFlagRepository(repository: DefaultFeatureFlagRepository): FeatureFlagRepository

    @Binds
    abstract fun bindStatisticsRepository(repository: DefaultStatisticsRepository): StatisticsRepository

    @Binds
    abstract fun bindGalleryFilesRepository(repository: DefaultGalleryFilesRepository): GalleryFilesRepository

    /**
     * Bind recent actions repository
     */
    @Binds
    abstract fun bindRecentActionsRepository(repository: DefaultRecentActionsRepository): RecentActionsRepository

    @Binds
    abstract fun bindSupportRepository(implementation: DefaultSupportRepository): SupportRepository

    @Binds
    abstract fun bindNotificationsRepository(repository: DefaultNotificationsRepository): NotificationsRepository

    /**
     * Bind sort order repository
     */
    @Binds
    abstract fun bindSortOrderRepository(repository: DefaultSortOrderRepository): SortOrderRepository

    @Binds
    abstract fun bindChatRepository(repository: DefaultChatRepository): ChatRepository

    @Binds
    abstract fun bindPushesRepository(repository: DefaultPushesRepository): PushesRepository

    /**
     * Bind domain transfers repository
     */
    @Binds
    abstract fun bindDomainTransfersRepository(repository: DefaultTransfersRepository): TransferRepository

    @Binds
    abstract fun bindCameraUploadRepository(repository: DefaultCameraUploadRepository): CameraUploadRepository

    @Binds
    abstract fun bindAlbumsRepository(repository: DefaultAlbumsRepository): AlbumsRepository

    @Binds
    abstract fun bindGlobalUpdatesRepository(repository: DefaultGlobalStatesRepository): GlobalStatesRepository

    @Binds
    abstract fun bindGetImageRepository(repository: DefaultImageRepository): ImageRepository

    @Binds
    abstract fun bindFilesRepository(repository: DefaultFilesRepository): FilesRepository

    @Binds
    abstract fun bindDomainFilesRepository(repository: DefaultFilesRepository): FileRepository

    @Binds
    @Singleton
    abstract fun bindAvatarRepository(repository: DefaultAvatarRepository): AvatarRepository

    @ExperimentalContracts
    @Singleton
    @Binds
    abstract fun bindSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindFavouritesRepository(repository: DefaultFavouritesRepository): FavouritesRepository
}