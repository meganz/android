package mega.privacy.android.app.di.imagepreview

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.multibindings.IntoMap
import mega.privacy.android.app.presentation.imagepreview.fetcher.AlbumContentImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.AlbumSharingImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.CloudDriveImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.DefaultImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.FavouriteImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.MediaDiscoveryImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.OfflineImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.PublicFileImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.TimelineImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource

@Module
@InstallIn(ViewModelComponent::class)
internal interface ImagePreviewModule {
    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.DEFAULT)
    fun DefaultImageNodeFetcher.bindDefaultFetcher(): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.TIMELINE)
    fun TimelineImageNodeFetcher.bindTimelineFetcher(): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.ALBUM_CONTENT)
    fun AlbumContentImageNodeFetcher.bindAlbumContentFetcher(): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.MEDIA_DISCOVERY)
    fun MediaDiscoveryImageNodeFetcher.bindMediaDiscoveryFetcher(): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.CLOUD_DRIVE)
    fun CloudDriveImageNodeFetcher.bindCloudDriveFetcher(): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.ALBUM_SHARING)
    fun AlbumSharingImageNodeFetcher.bindAlbumSharingFetcher(): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.OFFLINE)
    fun OfflineImageNodeFetcher.bindOfflineFetcher(): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.FAVOURITE)
    fun FavouriteImageNodeFetcher.bindFavouriteFetcher(): ImageNodeFetcher

    @Binds
    @IntoMap
    @ImageNodeFetcherSourceKey(ImagePreviewFetcherSource.PUBLIC_FILE)
    fun PublicFileImageNodeFetcher.bindPublicFileFetcher(): ImageNodeFetcher
}
