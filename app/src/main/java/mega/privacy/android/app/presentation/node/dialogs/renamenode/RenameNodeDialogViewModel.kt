package mega.privacy.android.app.presentation.node.dialogs.renamenode

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
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.domain.usecase.node.ValidNameType
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogAction.OnRenameConfirmed
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogAction.OnRenameSucceeded
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogAction.OnLoadNodeName
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogAction.OnRenameValidationPassed
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogAction.OnChangeNodeExtensionDialogShown
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class RenameNodeDialogViewModel @Inject constructor(
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val checkForValidNameUseCase: CheckForValidNameUseCase,
    private val renameNodeUseCase: RenameNodeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RenameNodeDialogState())
    val state = _state.asStateFlow()

    fun handleAction(action: RenameNodeDialogAction) {
        when (action) {
            is OnLoadNodeName -> {
                viewModelScope.launch {
                    runCatching {
                        getNodeByHandleUseCase(action.nodeId)
                    }.onSuccess { node ->
                        _state.update {
                            it.copy(
                                nodeName = node?.name,
                                errorMessage = null
                            )
                        }
                    }
                }
            }

            is OnRenameConfirmed -> {
                viewModelScope.launch {
                    getNodeByHandleUseCase(action.nodeId)?.let { currentNode ->
                        handleValidationResult(action, currentNode)
                    }
                }
            }

            is OnRenameSucceeded -> {
                _state.update { it.copy(renameSuccessfulEvent = consumed) }
            }

            is OnRenameValidationPassed -> {
                _state.update { it.copy(renameValidationPassedEvent = consumed) }
            }

            is OnChangeNodeExtensionDialogShown -> {
                _state.update { it.copy(showChangeNodeExtensionDialogEvent = consumed()) }
            }
        }
    }

    private suspend fun handleValidationResult(
        action: OnRenameConfirmed,
        currentNode: UnTypedNode,
    ) {
        when (checkForValidNameUseCase(action.newNodeName, currentNode)) {
            ValidNameType.BLANK_NAME -> {
                _state.update { it.copy(errorMessage = R.string.invalid_string) }
            }

            ValidNameType.INVALID_NAME -> {
                _state.update { it.copy(errorMessage = R.string.invalid_characters_defined) }
            }

            ValidNameType.NAME_ALREADY_EXISTS -> {
                _state.update { it.copy(errorMessage = R.string.same_file_name_warning) }
            }

            ValidNameType.NO_EXTENSION -> {
                _state.update { it.copy(errorMessage = R.string.file_without_extension_warning) }
            }

            ValidNameType.DIFFERENT_EXTENSION -> {
                _state.update { it.copy(showChangeNodeExtensionDialogEvent = triggered(action.newNodeName)) }
            }

            else -> {
                _state.update {
                    it.copy(
                        renameValidationPassedEvent = triggered
                    )
                }
                renameNode(action.nodeId, action.newNodeName)
            }
        }
    }

    private fun renameNode(nodeId: Long, newNodeName: String) {
        viewModelScope.launch {
            runCatching {
                renameNodeUseCase(nodeId, newNodeName)
            }.onSuccess {
                _state.update { it.copy(renameSuccessfulEvent = triggered) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}
