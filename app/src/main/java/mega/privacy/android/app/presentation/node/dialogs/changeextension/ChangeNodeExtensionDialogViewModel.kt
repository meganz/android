package mega.privacy.android.app.presentation.node.dialogs.changeextension

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ChangeNodeExtensionDialogViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val renameNodeUseCase: RenameNodeUseCase,
    private val snackBarHandler: SnackBarHandler,
) : ViewModel() {

    fun handleAction(action: ChangeNodeExtensionAction) {
        when (action) {
            is ChangeNodeExtensionAction.OnChangeExtensionConfirmed -> {
                applicationScope.launch {
                    runCatching {
                        renameNodeUseCase(
                            action.nodeId,
                            action.newNodeName,
                        )
                    }.onSuccess {
                        snackBarHandler.postSnackbarMessage(R.string.context_correctly_renamed)
                    }.onFailure {
                        Timber.e(it, "Error renaming node")
                    }
                }
            }
        }
    }
}