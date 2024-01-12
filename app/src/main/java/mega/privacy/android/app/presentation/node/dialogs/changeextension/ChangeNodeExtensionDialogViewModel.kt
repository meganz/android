package mega.privacy.android.app.presentation.node.dialogs.changeextension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ChangeNodeExtensionDialogViewModel @Inject constructor(
    private val renameNodeUseCase: RenameNodeUseCase,
) : ViewModel() {

    private var _state = MutableStateFlow(ChangeNodeExtensionState())
    val state = _state.asStateFlow()

    fun handleAction(action: ChangeNodeExtensionAction) {
        when (action) {
            is ChangeNodeExtensionAction.OnChangeExtensionConfirmed -> {
                viewModelScope.launch {
                    runCatching {
                        renameNodeUseCase(
                            action.nodeId,
                            action.newNodeName,
                        )
                    }
                        .onSuccess {
                            _state.update {
                                it.copy(
                                    renameSuccessfulEvent = triggered
                                )
                            }
                        }
                        .onFailure {
                            Timber.e(it, "Error renaming node")
                        }
                }
            }

            ChangeNodeExtensionAction.OnChangeExtensionConsumed -> {
                _state.update {
                    it.copy(
                        renameSuccessfulEvent = consumed
                    )
                }
            }
        }
    }
}