package mega.privacy.android.app.presentation.rubbishbin

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.usecase.*
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
class RubbishBinViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    @MegaApi megaApi: MegaApiAndroid,
    sortOrderManagement: SortOrderManagement,
    private val getManagerParentHandle: GetManagerParentHandle,
) : ViewModel() {

    /**
     * Accessors to the current rubbish parent handle set in memory
     */
    val parentHandle: Long
        get() = getManagerParentHandle(GetManagerParentHandleType.RubbishBin)

    /**
     * Monitor global node updates and dispatch to observers
     */
    val updateNodes: LiveData<List<MegaNode>> =
        monitorNodeUpdates()
            .also { Timber.d("onNodesUpdate") }
            .filterNot { it.isEmpty() }
            .map {
                if (parentHandle == -1L) {
                    megaApi.getChildren(megaApi.rubbishNode, sortOrderManagement.getOrderCloud())
                }
                else {
                    megaApi.getChildren(
                        megaApi.getNodeByHandle(parentHandle),
                        sortOrderManagement.getOrderCloud()
                    )
                }
            }
            .asLiveData()

}