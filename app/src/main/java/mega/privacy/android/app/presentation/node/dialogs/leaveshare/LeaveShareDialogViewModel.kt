package mega.privacy.android.app.presentation.node.dialogs.leaveshare

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.movenode.mapper.LeaveShareRequestMessageMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.node.LeaveSharesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for Leave Share dialog
 * @property leaveSharesUseCase [LeaveSharesUseCase]
 * @property applicationScope [CoroutineScope]
 * @property leaveShareRequestMessageMapper [LeaveShareRequestMessageMapper]
 * @property snackBarHandler [SnackBarHandler]
 */
@HiltViewModel
class LeaveShareDialogViewModel @Inject constructor(
    private val leaveSharesUseCase: LeaveSharesUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val leaveShareRequestMessageMapper: LeaveShareRequestMessageMapper,
    private val snackBarHandler: SnackBarHandler,
) : ViewModel() {

    /**
     * on leave share confirm clicked
     */
    fun onLeaveShareConfirmClicked(handles: List<Long>) {
        applicationScope.launch {
            val nodeIds = handles.map {
                NodeId(it)
            }
            runCatching { leaveSharesUseCase(nodeIds) }
                .onSuccess {
                    val message = leaveShareRequestMessageMapper(it)
                    snackBarHandler.postSnackbarMessage(message = message)
                }.onFailure {
                    Timber.e(it)
                }

        }
    }
}