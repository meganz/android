package mega.privacy.android.app.di.imagepreview

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.multibindings.IntoMap
import mega.privacy.android.app.presentation.imagepreview.fetcher.AlbumContentImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.AlbumSharingImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.BackupsImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.ChatImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.CloudDriveImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.ContactFileListImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.DefaultImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.FavouriteImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.FolderLinkImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.FolderLinkMediaDiscoveryImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.MediaDiscoveryImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.OfflineImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.PublicFileImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.RubbishBinImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.SharedItemsImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.TimelineImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.ZipImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource

@Module
@InstallIn(ViewModelComponent::class)
internal interface ImagePreviewModule {
    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.DEFAULT)
    fun bindDefaultFetcher(fetcher: DefaultImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.TIMELINE)
    fun bindTimelineFetcher(fetcher: TimelineImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.ALBUM_CONTENT)
    fun bindAlbumContentFetcher(fetcher: AlbumContentImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.MEDIA_DISCOVERY)
    fun bindMediaDiscoveryFetcher(fetcher: MediaDiscoveryImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.CLOUD_DRIVE)
    fun bindCloudDriveFetcher(fetcher: CloudDriveImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.ALBUM_SHARING)
    fun bindAlbumSharingFetcher(fetcher: AlbumSharingImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.OFFLINE)
    fun bindOfflineFetcher(fetcher: OfflineImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.FAVOURITE)
    fun bindFavouriteFetcher(fetcher: FavouriteImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.PUBLIC_FILE)
    fun bindPublicFileFetcher(fetcher: PublicFileImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.FOLDER_LINK)
    fun bindFolderLinkFetcher(fetcher: FolderLinkImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.FOLDER_LINK_MEDIA_DISCOVERY)
    fun bindFolderLinkMediaDiscoveryFetcher(fetcher: FolderLinkMediaDiscoveryImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.SHARED_ITEMS)
    fun bindSharedItemsFetcher(fetcher: SharedItemsImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.BACKUPS)
    fun bindBackupsFetcher(fetcher: BackupsImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.RUBBISH_BIN)
    fun bindRubbishBinFetcher(fetcher: RubbishBinImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.ZIP)
    fun bindZipFetcher(fetcher: ZipImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.CONTACT_FILE_LIST)
    fun bindContactFileListFetcher(fetcher: ContactFileListImageNodeFetcher): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.CHAT)
    fun bindChatFetcher(fetcher: ChatImageNodeFetcher): ImageNodeFetcher
}