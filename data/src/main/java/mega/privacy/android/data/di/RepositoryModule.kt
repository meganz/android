package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.repository.AndroidBillingRepository
import mega.privacy.android.data.repository.CallRepositoryImpl
import mega.privacy.android.data.repository.ChatRepositoryImpl
import mega.privacy.android.data.repository.DefaultAlbumRepository
import mega.privacy.android.data.repository.DefaultAvatarRepository
import mega.privacy.android.data.repository.DefaultBillingRepository
import mega.privacy.android.data.repository.DefaultCameraUploadRepository
import mega.privacy.android.data.repository.DefaultCancelTokenRepository
import mega.privacy.android.data.repository.DefaultChatParticipantsRepository
import mega.privacy.android.data.repository.DefaultClipboardRepository
import mega.privacy.android.data.repository.DefaultContactsRepository
import mega.privacy.android.data.repository.DefaultFavouritesRepository
import mega.privacy.android.data.repository.DefaultFeatureFlagRepository
import mega.privacy.android.data.repository.DefaultGalleryFilesRepository
import mega.privacy.android.data.repository.DefaultGlobalStatesRepository
import mega.privacy.android.data.repository.DefaultImageRepository
import mega.privacy.android.data.repository.DefaultLoginRepository
import mega.privacy.android.data.repository.DefaultMediaPlayerRepository
import mega.privacy.android.data.repository.DefaultNetworkRepository
import mega.privacy.android.data.repository.DefaultNotificationsRepository
import mega.privacy.android.data.repository.DefaultPhotosRepository
import mega.privacy.android.data.repository.DefaultPushesRepository
import mega.privacy.android.data.repository.DefaultQRCodeRepository
import mega.privacy.android.data.repository.DefaultRecentActionsRepository
import mega.privacy.android.data.repository.DefaultSettingsRepository
import mega.privacy.android.data.repository.DefaultSortOrderRepository
import mega.privacy.android.data.repository.DefaultStatisticsRepository
import mega.privacy.android.data.repository.DefaultSupportRepository
import mega.privacy.android.data.repository.DefaultTimeSystemRepository
import mega.privacy.android.data.repository.DefaultTransfersRepository
import mega.privacy.android.data.repository.DefaultVerificationRepository
import mega.privacy.android.data.repository.EnvironmentRepositoryImpl
import mega.privacy.android.data.repository.FileLinkRepositoryImpl
import mega.privacy.android.data.repository.FileSystemRepositoryImpl
import mega.privacy.android.data.repository.FolderLinkRepositoryImpl
import mega.privacy.android.data.repository.GlobalStatesRepository
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.data.repository.MegaNodeRepositoryImpl
import mega.privacy.android.data.repository.NodeRepositoryImpl
import mega.privacy.android.data.repository.SlideshowRepositoryImpl
import mega.privacy.android.data.repository.StreamingServerRepositoryImpl
import mega.privacy.android.data.repository.ViewTypeRepositoryImpl
import mega.privacy.android.data.repository.account.BusinessRepositoryImpl
import mega.privacy.android.data.repository.account.DefaultAccountRepository
import mega.privacy.android.data.repository.filemanagement.ShareRepositoryImpl
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.repository.BusinessRepository
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.CancelTokenRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ClipboardRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.FeatureFlagRepository
import mega.privacy.android.domain.repository.FileLinkRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.repository.GalleryFilesRepository
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.repository.QRCodeRepository
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.repository.SlideshowRepository
import mega.privacy.android.domain.repository.SortOrderRepository
import mega.privacy.android.domain.repository.StatisticsRepository
import mega.privacy.android.domain.repository.StreamingServerRepository
import mega.privacy.android.domain.repository.SupportRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.repository.VerificationRepository
import mega.privacy.android.domain.repository.ViewTypeRepository
import mega.privacy.android.domain.repository.filemanagement.ShareRepository
import javax.inject.Singleton
import kotlin.contracts.ExperimentalContracts

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    abstract fun bindSlideshowRepository(implementation: SlideshowRepositoryImpl): SlideshowRepository

    @Binds
    abstract fun bindAlbumRepository(repository: DefaultAlbumRepository): AlbumRepository

    @Binds
    abstract fun bindContactsRepository(repository: DefaultContactsRepository): ContactsRepository

    @Binds
    abstract fun bindNetworkRepository(repository: DefaultNetworkRepository): NetworkRepository

    @Binds
    abstract fun bindDeviceRepository(implementation: EnvironmentRepositoryImpl): EnvironmentRepository

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
    abstract fun bindChatRepository(repository: ChatRepositoryImpl): ChatRepository

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
    abstract fun bindGlobalUpdatesRepository(repository: DefaultGlobalStatesRepository): GlobalStatesRepository

    @Binds
    abstract fun bindGetImageRepository(repository: DefaultImageRepository): ImageRepository

    @Binds
    abstract fun bindFilesRepository(repository: MegaNodeRepositoryImpl): MegaNodeRepository

    @Binds
    abstract fun bindDomainFilesRepository(repository: FileSystemRepositoryImpl): FileSystemRepository

    @Binds
    abstract fun bindNodeRepository(repository: NodeRepositoryImpl): NodeRepository

    @Binds
    @Singleton
    abstract fun bindAvatarRepository(repository: DefaultAvatarRepository): AvatarRepository

    @ExperimentalContracts
    @Singleton
    @Binds
    abstract fun bindSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindFavouritesRepository(repository: DefaultFavouritesRepository): FavouritesRepository

    @ExperimentalContracts
    @Binds
    abstract fun bindAccountRepository(repository: DefaultAccountRepository): AccountRepository

    @Binds
    abstract fun bindTimeSystemRepository(repository: DefaultTimeSystemRepository): TimeSystemRepository

    @Binds
    @Singleton
    abstract fun bindMediaPlayerRepository(repository: DefaultMediaPlayerRepository): MediaPlayerRepository

    @Binds
    abstract fun bindLoginRepository(repository: DefaultLoginRepository): LoginRepository

    @Binds
    abstract fun bindPhotosRepository(repository: DefaultPhotosRepository): PhotosRepository

    @Singleton
    @Binds
    abstract fun bindVerificationRepository(repository: DefaultVerificationRepository): VerificationRepository

    @Binds
    abstract fun bindBillingRepository(repository: DefaultBillingRepository): BillingRepository

    @Binds
    abstract fun bindChatParticipantsRepository(repository: DefaultChatParticipantsRepository): ChatParticipantsRepository

    @Binds
    abstract fun bindCallRepository(repository: CallRepositoryImpl): CallRepository

    @ExperimentalContracts
    @Binds
    abstract fun bindQRCodeRepository(repository: DefaultQRCodeRepository): QRCodeRepository

    @Binds
    abstract fun bindClipboardRepository(repository: DefaultClipboardRepository): ClipboardRepository

    @Binds
    abstract fun bindAndroidBillingRepository(repository: DefaultBillingRepository): AndroidBillingRepository

    @Binds
    abstract fun bindStreamingServerRepository(implementation: StreamingServerRepositoryImpl): StreamingServerRepository

    @Binds
    abstract fun bindViewTypeRepository(implementation: ViewTypeRepositoryImpl): ViewTypeRepository

    @Binds
    abstract fun bindFolderLinkRepository(implementation: FolderLinkRepositoryImpl): FolderLinkRepository

    @Binds
    abstract fun bindBusinessRepository(implementation: BusinessRepositoryImpl): BusinessRepository

    @Binds
    abstract fun bindShareRepository(implementation: ShareRepositoryImpl): ShareRepository

    @Binds
    abstract fun bindCancelTokenRepository(implementation: DefaultCancelTokenRepository): CancelTokenRepository

    @Binds
    abstract fun bindFileLinkRepository(implementation: FileLinkRepositoryImpl): FileLinkRepository

}
