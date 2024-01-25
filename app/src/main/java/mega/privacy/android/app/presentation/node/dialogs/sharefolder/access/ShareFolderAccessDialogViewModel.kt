package mega.privacy.android.app.presentation.node.dialogs.sharefolder.access

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.foldernode.ShareFolderUseCase
import javax.inject.Inject

/**
 * View model for share folder access dialog
 * @property applicationScope [CoroutineScope]
 * @property sharedFolderUseCase [ShareFolderUseCase]
 * @property moveRequestMessageMapper [MoveRequestMessageMapper]
 * @property snackBarHandler [SnackBarHandler]
 */
@HiltViewModel
class ShareFolderAccessDialogViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val sharedFolderUseCase: ShareFolderUseCase,
    private val moveRequestMessageMapper: MoveRequestMessageMapper,
    private val snackBarHandler: SnackBarHandler,
) : ViewModel() {

    /**
     * Share folders to contacts with access rights
     * @param handles list of folders to be shared
     * @param contactData list of users
     * @param accessPermission [AccessPermission]
     */
    fun shareFolder(
        handles: List<Long>,
        contactData: List<String>,
        accessPermission: AccessPermission,
    ) {
        applicationScope.launch {
            val nodeIds = handles.map {
                NodeId(it)
            }
            val result = sharedFolderUseCase(
                nodeIds = nodeIds,
                contactData = contactData,
                accessPermission = accessPermission
            )
            val message = moveRequestMessageMapper(result)
            snackBarHandler.postSnackbarMessage(message)
        }
    }
}