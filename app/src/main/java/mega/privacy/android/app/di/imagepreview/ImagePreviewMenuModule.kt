package mega.privacy.android.app.di.imagepreview

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.multibindings.IntoMap
import mega.privacy.android.app.presentation.imagepreview.menu.AlbumContentImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.AlbumSharingImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.BackupsImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.ChatImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.CloudDriveImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.ContactFileListImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.DefaultImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.FavouriteImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.FileImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.FolderLinkImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.ImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.LinksImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.MediaDiscoveryImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.OfflineImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.PublicFileImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.RubbishBinImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.SharedItemsImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.TimelineImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.ZipImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource

@Module
@InstallIn(ViewModelComponent::class)
internal interface ImagePreviewMenuModule {
    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.DEFAULT)
    fun bindDefaultMenu(menu: DefaultImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.TIMELINE)
    fun bindTimelineImagePreviewMenu(menu: TimelineImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.ALBUM_CONTENT)
    fun bindAlbumContentImagePreviewMenu(menu: AlbumContentImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.MEDIA_DISCOVERY)
    fun bindMediaDiscoveryMenu(menu: MediaDiscoveryImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.CLOUD_DRIVE)
    fun bindCloudDriveMenu(menu: CloudDriveImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.ALBUM_SHARING)
    fun bindAlbumSharingMenu(menu: AlbumSharingImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.OFFLINE)
    fun bindOfflineMenu(menu: OfflineImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.FAVOURITE)
    fun bindFavouriteMenu(menu: FavouriteImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.FILE)
    fun bindFileMenu(menu: FileImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.PUBLIC_FILE)
    fun bindPublicFileMenu(menu: PublicFileImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.FOLDER_LINK)
    fun bindFolderLinkMenu(menu: FolderLinkImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.SHARED_ITEMS)
    fun bindSharedItemsMenu(menu: SharedItemsImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.LINKS)
    fun bindLinksMenu(menu: LinksImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.BACKUPS)
    fun bindBackupsMenu(menu: BackupsImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.RUBBISH_BIN)
    fun bindRubbishBinMenu(menu: RubbishBinImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.ZIP)
    fun bindZipMenu(menu: ZipImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.CONTACT_FILE_LIST)
    fun bindContactFileListMenu(menu: ContactFileListImagePreviewMenu): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.CHAT)
    fun bindChatMenu(menu: ChatImagePreviewMenu): ImagePreviewMenu
}
