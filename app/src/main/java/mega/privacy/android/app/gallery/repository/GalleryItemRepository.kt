package mega.privacy.android.app.gallery.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryItemRepository @Inject constructor(
    @ApplicationContext val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid
) {
    /** Live Data to notify the query result*/
    var galleryItems: LiveData<List<GalleryItem>> = MutableLiveData()

    /** Current effective NodeFetcher */
    lateinit var nodesFetcher: GalleryNodeFetcher

    /** The selected nodes in action mode */
    private val selectedNodesMap: LinkedHashMap<Any, GalleryItem> = LinkedHashMap()

    suspend fun getFiles(zoom: Int = ZoomUtil.ZOOM_DEFAULT) {
        preserveSelectedItems()

        // Create a node fetcher for the new request, and link fileNodeItems to its result.
        // Then the result of any previous NodesFetcher will be ignored
        nodesFetcher = GalleryNodeFetcher(context, megaApi, selectedNodesMap, zoom)
        galleryItems = nodesFetcher.result

        withContext(Dispatchers.IO) {
            nodesFetcher.getGalleryItems()
        }
    }

    suspend fun getPreviews(map: MutableMap<MegaNode, String>, refreshCallback: () -> Unit) {
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
}
