package mega.privacy.android.app.di.imagepreview

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.multibindings.IntoMap
import mega.privacy.android.app.presentation.imagepreview.menu.AlbumContentImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.AlbumSharingImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.CloudDriveImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.DefaultImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.FavouriteImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.FileImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.FolderLinkImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.ImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.MediaDiscoveryImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.OfflineImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.PublicFileImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.SharedItemsImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.TimelineImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource

@Module
@InstallIn(ViewModelComponent::class)
internal interface ImagePreviewMenuModule {
    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.DEFAULT)
    fun DefaultImagePreviewMenu.bindDefaultMenu(): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.TIMELINE)
    fun TimelineImagePreviewMenu.bindTimelineImagePreviewMenu(): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.ALBUM_CONTENT)
    fun AlbumContentImagePreviewMenu.bindAlbumContentImagePreviewMenu(): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.MEDIA_DISCOVERY)
    fun MediaDiscoveryImagePreviewMenu.bindMediaDiscoveryMenu(): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.CLOUD_DRIVE)
    fun CloudDriveImagePreviewMenu.bindCloudDriveMenu(): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.ALBUM_SHARING)
    fun AlbumSharingImagePreviewMenu.bindAlbumSharingMenu(): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.OFFLINE)
    fun OfflineImagePreviewMenu.bindOfflineMenu(): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.FAVOURITE)
    fun FavouriteImagePreviewMenu.bindFavouriteMenu(): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.FILE)
    fun FileImagePreviewMenu.bindFileMenu(): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.PUBLIC_FILE)
    fun PublicFileImagePreviewMenu.bindPublicFileMenu(): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.FOLDER_LINK)
    fun FolderLinkImagePreviewMenu.bindFolderLinkMenu(): ImagePreviewMenu

    @Binds
    @IntoMap
    @ImagePreviewMenuFeatureKey(ImagePreviewMenuSource.SHARED_ITEMS)
    fun SharedItemsImagePreviewMenu.bindSharedItemsMenu(): ImagePreviewMenu
}
