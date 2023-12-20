package mega.privacy.android.app.di.imagepreview

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.multibindings.IntoMap
import mega.privacy.android.app.presentation.imagepreview.menu.AlbumContentImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.AlbumSharingImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.CloudDriveImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.ImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.MediaDiscoveryImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.OfflineImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.menu.TimelineImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource

@Module
@InstallIn(ViewModelComponent::class)
internal interface ImagePreviewMenuModule {
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
}
