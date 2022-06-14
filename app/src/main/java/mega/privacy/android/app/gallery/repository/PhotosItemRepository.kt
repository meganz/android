package mega.privacy.android.app.gallery.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.extension.previewPath
import mega.privacy.android.app.gallery.repository.fetcher.GalleryBaseFetcher
import mega.privacy.android.app.gallery.repository.fetcher.PhotosFetcher
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotosItemRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbHandler: DatabaseHandler,
    cacheFolderGateway: CacheFolderGateway,
) {

    fun getPublicLinks(): ArrayList<MegaNode> {
        return megaApi.publicLinks
    }

    fun buildDefaultDownloadDir(): File {
        return FileUtil.buildDefaultDownloadDir(context)
    }

    /** Live Data to notify the query result*/
    var galleryItems: LiveData<List<GalleryItem>> = MutableLiveData()

    /** Current effective NodeFetcher */
    private lateinit var nodesFetcher: PhotosFetcher

    /** The selected nodes in action mode */
    private val selectedNodesMap: LinkedHashMap<Any, GalleryItem> =
        LinkedHashMap()

    val previewFolder = cacheFolderGateway.getCacheFolder(CacheFolderManager.PREVIEW_FOLDER)

    /**
     * Gets the image items.
     *
     * @param cancelToken   MegaCancelToken to cancel the search at any time.
     * @param order         Order to get the items.
     * @param zoom          Zoom value.
     */
    suspend fun getFiles(
        cancelToken: MegaCancelToken,
        order: Int,
        zoom: Int,
    ) {
        preserveSelectedItems()

        // Create a node fetcher for the new request, and link fileNodeItems to its result.
        // Then the result of any previous NodesFetcher will be ignored
        nodesFetcher = PhotosFetcher(context, megaApi, selectedNodesMap, order, zoom,
            this.dbHandler
        )

        @Suppress("UNCHECKED_CAST")
        galleryItems = nodesFetcher.result as MutableLiveData<List<GalleryItem>>

        withContext(Dispatchers.IO) {
            nodesFetcher.getGalleryItems(cancelToken)
        }
    }

    suspend fun getPreviews(list: List<GalleryCard>, refreshCallback: () -> Unit) {
        val missingPreviewIds = list.filter { it.preview == null }
            .map { it.id }

        galleryItems.value?.let { items ->
            val map = items.mapNotNull { it.node }
                .filter { missingPreviewIds.contains(it.handle) }
                .associateWith { File(previewFolder, it.previewPath).absolutePath }

            nodesFetcher.getPreviewsFromServer(map, refreshCallback)
        }
    }

    fun emitFiles() {
        nodesFetcher.result.value?.let {
            nodesFetcher.result.value = it
        }
    }

    /**
     * Preserve those action mode "selected" nodes.
     * In order to restore their "selected" status in event of querying the raw data again
     */
    private fun preserveSelectedItems() {
        selectedNodesMap.clear()
        val listNodeItem = galleryItems.value ?: return

        for (item in listNodeItem) {
            if (item.selected) {
                item.node?.let {
                    selectedNodesMap[it.handle] = item
                }
            }
        }
    }
}
