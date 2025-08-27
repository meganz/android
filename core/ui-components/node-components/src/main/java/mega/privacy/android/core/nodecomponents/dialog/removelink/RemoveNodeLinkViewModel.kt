package mega.privacy.android.core.nodecomponents.dialog.removelink

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.RemovePublicLinkResultMapper
import mega.privacy.android.core.sharedcomponents.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model to remove link
 */
@HiltViewModel
class RemoveNodeLinkViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val disableExportNodesUseCase: DisableExportNodesUseCase,
    private val removePublicLinkResultMapper: RemovePublicLinkResultMapper,
    private val snackBarHandler: SnackBarHandler
) : ViewModel() {

    /**
     * Disable export nodes
     * @param nodeIds
     */
    fun disableExport(nodeIds: List<Long>) {
        applicationScope.launch {
            runCatching {
                disableExportNodesUseCase(nodeIds.map { NodeId(it) })
            }.onFailure {
                Timber.e(it)
            }.onSuccess { result ->
                val message = removePublicLinkResultMapper(result)
                snackBarHandler.postSnackbarMessage(message)
            }
        }
    }
}