package mega.privacy.android.app.gallery.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.featuretoggle.PhotosFeatureToggle
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.fetcher.GalleryBaseFetcher
import mega.privacy.android.app.gallery.repository.fetcher.NewPhotosFetcher
import mega.privacy.android.app.gallery.repository.fetcher.PhotosFetcher
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
    dbHandler: DatabaseHandler
) : GalleryItemRepository(context, megaApi, dbHandler) {

    fun getPublicLinks():ArrayList<MegaNode>{
        return megaApi.publicLinks
    }

    fun buildDefaultDownloadDir(): File {
        return FileUtil.buildDefaultDownloadDir(context)
    }

    override fun initGalleryNodeFetcher(
        context: Context,
        megaApi: MegaApiAndroid,
        selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
        order: Int,
        zoom: Int,
        dbHandler: DatabaseHandler,
        handle: Long?
    ): GalleryBaseFetcher {
        if (PhotosFeatureToggle.enabled) {
            return NewPhotosFetcher(context, megaApi, selectedNodesMap, order, zoom,
                this.dbHandler
            )
        }
        return PhotosFetcher(context, megaApi, selectedNodesMap, order, zoom,
            this.dbHandler
        )
    }
}
