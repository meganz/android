package mega.privacy.android.app.presentation.filebrowser

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [mega.privacy.android.app.main.managerSections.FileBrowserFragment]
 *
 * @param monitorNodeUpdates Monitor global node updates
 * @param megaApi
 * @param sortOrderManagement
 */
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    @MegaApi megaApi: MegaApiAndroid,
    sortOrderManagement: SortOrderManagement,
) : ViewModel() {

    /**
     * Current file browser parent handle
     */
    var parentHandle: Long = -1L

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
