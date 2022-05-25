package mega.privacy.android.app.gallery.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.fetcher.GalleryBaseFetcher
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import java.util.*

/**
 * The repository to handle gallery data logic
 */
abstract class GalleryItemRepository constructor(
    val context: Context,
    val megaApi: MegaApiAndroid,
    val dbHandler: DatabaseHandler
) {
    /** Live Data to notify the query result*/
    var galleryItems: LiveData<List<GalleryItem>> = MutableLiveData()

    /** Current effective NodeFetcher */
    lateinit var nodesFetcher: GalleryBaseFetcher

    /** The selected nodes in action mode */
    private val selectedNodesMap: LinkedHashMap<Any, GalleryItem> = LinkedHashMap()

    /**
     * Gets the image items.
     *
     * @param cancelToken   MegaCancelToken to cancel the search at any time.
     * @param order         Order to get the items.
     * @param zoom          Zoom value.
     * @param handle
     */
    suspend fun getFiles(
        cancelToken: MegaCancelToken,
        order: Int,
        zoom: Int,
        handle: Long? = null
    ) {
        preserveSelectedItems()

        // Create a node fetcher for the new request, and link fileNodeItems to its result.
        // Then the result of any previous NodesFetcher will be ignored
        nodesFetcher = initGalleryNodeFetcher(
            context,
            megaApi,
            selectedNodesMap,
            order,
            zoom,
            dbHandler,
            handle
        )
        @Suppress("UNCHECKED_CAST")
        galleryItems = nodesFetcher.result as MutableLiveData<List<GalleryItem>>

        withContext(Dispatchers.IO) {
            nodesFetcher.getGalleryItems(cancelToken)
        }
    }

    suspend fun getPreviews(map: Map<MegaNode, String>, refreshCallback: () -> Unit) {
        nodesFetcher.getPreviewsFromServer(map, refreshCallback)
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

    abstract fun initGalleryNodeFetcher(
        context: Context,
        megaApi: MegaApiAndroid,
        selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
        order: Int,
        zoom: Int,
        dbHandler: DatabaseHandler,
        handle: Long? = null
    ): GalleryBaseFetcher
}
