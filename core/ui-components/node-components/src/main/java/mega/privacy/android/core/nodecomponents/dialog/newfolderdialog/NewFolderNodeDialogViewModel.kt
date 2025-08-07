package mega.privacy.android.core.nodecomponents.dialog.newfolderdialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.ValidateNodeNameUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the new folder dialog.
 * Handles validation and folder creation.
 */
@HiltViewModel
class NewFolderNodeDialogViewModel @Inject constructor(
    private val validateNodeNameUseCase: ValidateNodeNameUseCase,
    private val createFolderNodeUseCase: CreateFolderNodeUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewFolderDialogState())
    val uiState: StateFlow<NewFolderDialogState> = _uiState.asStateFlow()

    /**
     * Creates a folder after validation.
     *
     * @param folderName The folder name to create
     * @param parentNodeId The parent node ID where the folder will be created
     */
    fun createFolder(
        folderName: String,
        parentNodeId: NodeId,
    ) = viewModelScope.launch {
        runCatching {
            val trimmedFolderName = folderName.trim()
            val parentOrRootNodeId =
                if (parentNodeId.longValue != -1L) parentNodeId else getRootNodeUseCase()?.id
            validateNodeNameUseCase(trimmedFolderName, parentOrRootNodeId)
            runCatching { createFolderNodeUseCase(folderName, parentOrRootNodeId) }
                .getOrNull()
        }.onSuccess { folderId ->
            _uiState.update { it.copy(folderCreatedEvent = triggered(folderId)) }
        }.onFailure { e ->
            Timber.e(e)
            _uiState.update { it.copy(errorEvent = triggered(e)) }
        }
    }

    /**
     * Clears the error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorEvent = consumed()) }
    }

    /**
     * Clears the folder created event.
     */
    fun clearFolderCreatedEvent() {
        _uiState.update { it.copy(folderCreatedEvent = consumed()) }
    }
}

const val INVALID_CHARACTERS = "\" * / : < > ? \\ |"