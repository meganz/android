package mega.privacy.android.app.gallery.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.fetcher.MediaFetcher
import mega.privacy.android.app.gallery.repository.fetcher.GalleryBaseFetcher
import nz.mega.sdk.MegaApiAndroid
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaItemRepository @Inject constructor(
        @ApplicationContext context: Context,
        @MegaApi megaApi: MegaApiAndroid,
        mDbHandler: DatabaseHandler
) : GalleryItemRepository(context, megaApi, mDbHandler) {

    private var mHandle = 0L

    override fun initGalleryNodeFetcher(
            context: Context,
            megaApi: MegaApiAndroid,
            selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
            order: Int,
            zoom: Int,
            dbHandler: DatabaseHandler
    ): GalleryBaseFetcher {
        return MediaFetcher(context, megaApi, selectedNodesMap, order, zoom, mHandle)
    }

    fun setCurrentHandle(handle: Long) {
        mHandle = handle
    }
}
