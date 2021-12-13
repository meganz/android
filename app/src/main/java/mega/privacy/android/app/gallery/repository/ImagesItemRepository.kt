package mega.privacy.android.app.gallery.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagesItemRepository @Inject constructor(
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
        return GalleryTypeFetcher(context, megaApi, selectedNodesMap, zoom)
    }
}
