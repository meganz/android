package mega.privacy.android.app.gallery.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.fetcher.FavouriteAlbumFetcher
import mega.privacy.android.app.gallery.repository.fetcher.GalleryBaseFetcher
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FavouriteAlbumRepository is using to fetch FavouriteAlbum data
 */
@Singleton
class FavouriteAlbumRepository @Inject constructor(
        @ApplicationContext context: Context,
        @MegaApi megaApi: MegaApiAndroid,
        dbHandler: DatabaseHandler,
        cacheFolderGateway: CacheFolderGateway,
) : GalleryItemRepository(context, megaApi, dbHandler, cacheFolderGateway) {

    override fun initGalleryNodeFetcher(
            context: Context,
            megaApi: MegaApiAndroid,
            selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
            order: Int,
            zoom: Int,
            dbHandler: DatabaseHandler,
            handle: Long?
    ): GalleryBaseFetcher {
        return FavouriteAlbumFetcher(
                context,
                megaApi,
                selectedNodesMap,
                order,
                zoom,
                dbHandler
        )
    }
}
