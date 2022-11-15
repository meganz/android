package mega.privacy.android.app.fragments.homepage

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import java.util.*
import javax.inject.Inject

/**
 * TypedFilesRepository
 *
 * @param context : App Context
 * @param megaApi : [MegaApiAndroid]
 * @param sortOrderIntMapper: [SortOrderIntMapper]
 */
class TypedFilesRepository @Inject constructor(
    @ApplicationContext val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val sortOrderIntMapper: SortOrderIntMapper,
) {
    /** Live Data to notify the query result*/
    var fileNodeItems: LiveData<List<NodeItem>> = MutableLiveData()

    /** Current effective NodeFetcher */
    lateinit var nodesFetcher: TypedNodesFetcher

    /** The selected nodes in action mode */
    private val selectedNodesMap: LinkedHashMap<Any, NodeItem> = LinkedHashMap()

    /**
     * Using a node fetcher for the new request, and link fileNodeItems to its result.
     *
     * @param cancelToken   MegaCancelToken to cancel the fetch at any time.
     * @param type          Type of nodes.
     * @param order         SortOrder to get nodes.
     */
    suspend fun getFiles(
        cancelToken: MegaCancelToken,
        type: Int,
        order: SortOrder,
    ) {
        preserveSelectedItems()

        // Create a node fetcher for the new request, and link fileNodeItems to its result.
        // Then the result of any previous NodesFetcher will be ignored
        nodesFetcher = TypedNodesFetcher(context,
            megaApi,
            type,
            sortOrderIntMapper(order),
            selectedNodesMap)
        fileNodeItems = nodesFetcher.result

        withContext(Dispatchers.IO) {
            nodesFetcher.getNodeItems(cancelToken)
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
        val listNodeItem = fileNodeItems.value ?: return

        for (item in listNodeItem) {
            if (item.selected) {
                item.node?.let {
                    selectedNodesMap[it.handle] = item
                }
            }
        }
    }
}
