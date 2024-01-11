package mega.privacy.android.app.presentation.node.dialogs

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
internal class RenameNodeDialogViewModel @Inject constructor(
    /// inject usecases here in next MR
) : ViewModel() {

    private val _state = MutableStateFlow(RenameNodeDialogState(nodeName = "Camera Uploads"))
    val state = _state.asStateFlow()

    // logic is handled in next MR
    fun handleAction(action: RenameNodeDialogAction) {
        when (action) {
            is RenameNodeDialogAction.OnLoadNodeNameDialog -> {

            }

            is RenameNodeDialogAction.OnRenameConfirmed -> {

            }

            RenameNodeDialogAction.OnRenameSucceeded -> {
                _state.update { it.copy(renameSuccessfulEvent = consumed()) }
            }

            RenameNodeDialogAction.OnRenameValidationPassed -> {
                _state.update { it.copy(renameValidationPassedEvent = consumed) }
            }
        }
    }
}