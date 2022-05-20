package mega.privacy.android.app.presentation.filebrowser

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.usecase.GetManagerParentHandle
import mega.privacy.android.app.domain.usecase.GetManagerParentHandleType
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [mega.privacy.android.app.main.managerSections.RubbishBinFragment]
 *
 * @param monitorNodeUpdates Monitor global node updates
 * @param megaApi
 * @param sortOrderManagement
 * @param getManagerParentHandle Get current parent handle set in manager section
 */
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    @MegaApi megaApi: MegaApiAndroid,
    sortOrderManagement: SortOrderManagement,
    private val getManagerParentHandle: GetManagerParentHandle,
) : ViewModel() {

    /**
     * Accessors to the current rubbish parent handle set in memory
     */
    val parentHandle: Long
        get() = getManagerParentHandle(GetManagerParentHandleType.Browser)

    /**
     * Monitor global node updates and dispatch to observers
     */
    val updateNodes: LiveData<List<MegaNode>> =
        monitorNodeUpdates()
            .also { Timber.d("onNodesUpdate") }
            .map {
                if (parentHandle == -1L) {
                    megaApi.rootNode?.let {
                        megaApi.getChildren(it, sortOrderManagement.getOrderCloud())
                    } ?: run {
                        emptyList()
                    }
                }
                else {
                    megaApi.getNodeByHandle(parentHandle)?.let {
                        megaApi.getChildren(it, sortOrderManagement.getOrderCloud())
                    } ?: run {
                        emptyList()
                    }
                }
            }
            .filterNot { it.isEmpty() }
            .asLiveData()
}
