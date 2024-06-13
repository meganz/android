package mega.privacy.android.app.presentation.offline.action

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.usecase.node.ExportNodesUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFilesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Reusable ViewModel to handle offline node related actions
 */
@HiltViewModel
class OfflineNodeActionsViewModel @Inject constructor(
    private val getOfflineFilesUseCase: GetOfflineFilesUseCase,
    private val exportNodesUseCase: ExportNodesUseCase,
    private val snackBarHandler: SnackBarHandler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineNodeActionsUiState())

    /**
     * Immutable UI State
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Handle share action of offline nodes
     */
    fun handleShareOfflineNodes(nodes: List<OfflineFileInformation>, isOnline: Boolean) {
        viewModelScope.launch {
            val isAllFiles = !nodes.any { it.isFolder }
            if (isAllFiles) {
                shareFiles(nodes)
            } else {
                if (isOnline) {
                    shareNodes(nodes)
                } else {
                    snackBarHandler.postSnackbarMessage(R.string.error_server_connection_problem)
                }
            }
        }
    }

    private suspend fun shareFiles(nodes: List<OfflineFileInformation>) {
        runCatching {
            getOfflineFilesUseCase(nodes)
        }.onSuccess { files ->
            _uiState.update {
                it.copy(shareFilesEvent = triggered(files))
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun shareNodes(nodes: List<OfflineFileInformation>) {
        runCatching {
            exportNodesUseCase(nodes.map { it.handle.toLong() })
        }.onSuccess { linksMap ->
            if (linksMap.isEmpty()) return@onSuccess

            val links = if (linksMap.size == 1) {
                linksMap.values.first()
            } else {
                // Format each link in a new line
                linksMap.values.joinToString("\n\n")
            }
            _uiState.update {
                it.copy(sharesNodeLinksEvent = triggered(nodes.first().name to links))
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Consume share files event
     */
    fun onShareFilesEventConsumed() {
        _uiState.update {
            it.copy(shareFilesEvent = consumed())
        }
    }

    /**
     * Consume share node links event
     */
    fun onShareNodeLinksEventConsumed() {
        _uiState.update {
            it.copy(sharesNodeLinksEvent = consumed())
        }
    }
}

