package mega.privacy.android.app.gallery.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.fetcher.GalleryMediaFetcher
import mega.privacy.android.app.gallery.repository.fetcher.GalleryNodeFetcher
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
        zoom: Int,
        dbHandler: DatabaseHandler
    ): GalleryNodeFetcher {
        return GalleryMediaFetcher(context, megaApi, selectedNodesMap, zoom,dbHandler, handle = mHandle)
    }

    fun setCurrentHandle(handle:Long){
        mHandle = handle
    }
}
