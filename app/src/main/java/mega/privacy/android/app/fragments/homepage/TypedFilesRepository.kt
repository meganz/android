package mega.privacy.android.app.fragments.homepage

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.di.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TypedFilesRepository @Inject constructor(
    @ApplicationContext val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid
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
     * @param order         Order to get nodes.
     */
    suspend fun getFiles(cancelToken: MegaCancelToken, type: Int, order: Int) {
        preserveSelectedItems()

        // Create a node fetcher for the new request, and link fileNodeItems to its result.
        // Then the result of any previous NodesFetcher will be ignored
        nodesFetcher = TypedNodesFetcher(context, megaApi, type, order, selectedNodesMap)
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
