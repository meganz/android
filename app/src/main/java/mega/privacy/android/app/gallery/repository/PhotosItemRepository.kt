package mega.privacy.android.app.gallery.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.fetcher.GalleryNodeFetcher
import mega.privacy.android.app.gallery.repository.fetcher.GalleryPhotosFetcher
import mega.privacy.android.app.utils.FileUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotosItemRepository @Inject constructor(
    @ApplicationContext context: Context,
    @MegaApi megaApi: MegaApiAndroid,
    mDbHandler: DatabaseHandler
) : GalleryItemRepository(context, megaApi, mDbHandler) {

    override fun initGalleryNodeFetcher(
        context: Context,
        megaApi: MegaApiAndroid,
        selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
        zoom: Int,
        dbHandler: DatabaseHandler
    ): GalleryNodeFetcher {
        return GalleryPhotosFetcher(context, megaApi, selectedNodesMap, zoom, mDbHandler)
    }


    fun getPublicLinks():ArrayList<MegaNode>{
        return megaApi.publicLinks
    }

    fun buildDefaultDownloadDir(): File {
        return FileUtil.buildDefaultDownloadDir(context)
    }
}
