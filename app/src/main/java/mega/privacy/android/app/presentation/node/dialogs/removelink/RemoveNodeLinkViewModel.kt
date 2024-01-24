package mega.privacy.android.app.presentation.node.dialogs.removelink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
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