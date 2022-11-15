package mega.privacy.android.app.gallery.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.extension.previewPath
import mega.privacy.android.app.gallery.repository.fetcher.MediaFetcher
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaApiAndroid
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaItemRepository
 * 
 * @param context            : App Context
 * @param megaApi            : [MegaApiAndroid]
 * @param cacheFolderGateway : [CacheFolderGateway]
 * @param sortOrderIntMapper : [SortOrderIntMapper]
 */
@Singleton
class MediaItemRepository @Inject constructor(
    @ApplicationContext val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    cacheFolderGateway: CacheFolderGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
) {

    /** Live Data to notify the query result*/
    var galleryItems: LiveData<List<GalleryItem>> = MutableLiveData()

    /** Current effective NodeFetcher */
    lateinit var nodesFetcher: MediaFetcher

    /** The selected nodes in action mode */
    private val selectedNodesMap: LinkedHashMap<Any, GalleryItem> = LinkedHashMap()

    val previewFolder = cacheFolderGateway.getCacheFolder(CacheFolderManager.PREVIEW_FOLDER)

    /**
     * Gets the image items.
     *
     * @param order         SortOrder to get the items.
     * @param zoom          Zoom value.
     * @param handle        Long
     */
    suspend fun getFiles(
        order: SortOrder,
        zoom: Int,
        handle: Long
    ) {
        preserveSelectedItems()

        // Create a node fetcher for the new request, and link fileNodeItems to its result.
        // Then the result of any previous NodesFetcher will be ignored
        nodesFetcher = MediaFetcher(context,
            megaApi,
            selectedNodesMap,
            sortOrderIntMapper(order),
            zoom,
            handle)
        @Suppress("UNCHECKED_CAST")
        galleryItems = nodesFetcher.result as MutableLiveData<List<GalleryItem>>

        withContext(Dispatchers.IO) {
            nodesFetcher.getGalleryItems()
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
