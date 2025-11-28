package mega.privacy.android.core.nodecomponents.dialog.rename

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogAction.OnChangeNodeExtensionDialogShown
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogAction.OnLoadNodeName
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogAction.OnRenameConfirmed
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogAction.OnRenameValidationPassed
import mega.privacy.android.core.sharedcomponents.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.domain.usecase.node.ValidNameType
import mega.privacy.android.shared.resources.R as sharedResR
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RenameNodeDialogViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val checkForValidNameUseCase: CheckForValidNameUseCase,
    private val renameNodeUseCase: RenameNodeUseCase,
    private val snackBarHandler: SnackBarHandler,
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
                applicationScope.launch {
                    runCatching { getNodeByHandleUseCase(action.nodeId) }
                        .onSuccess {
                            it?.let { currentNode ->
                                handleValidationResult(action, currentNode)
                            }
                        }.onFailure {
                            Timber.e(it)
                        }
                }
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
                    it.copy(renameValidationPassedEvent = triggered)
                }

                renameNode(currentNode.id, action.newNodeName)
            }
        }
    }

    internal fun renameNode(nodeId: NodeId, newNodeName: String) {
        applicationScope.launch {
            runCatching {
                renameNodeUseCase(nodeId.longValue, newNodeName)
            }.onSuccess {
                snackBarHandler.postSnackbarMessage(
                    context.getString(sharedResR.string.context_correctly_renamed)
                )
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}
