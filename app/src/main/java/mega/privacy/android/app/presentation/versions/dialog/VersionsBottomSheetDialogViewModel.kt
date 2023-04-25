package mega.privacy.android.app.presentation.versions.dialog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.modalbottomsheet.VersionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.versions.dialog.model.VersionsBottomSheetDialogState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import javax.inject.Inject

/**
 * ViewModel class for [mega.privacy.android.app.modalbottomsheet.VersionsBottomSheetDialogFragment]
 *
 * @property getNodeAccessPermission Retrieves the Node Access Permission
 * @property getNodeByHandle Retrieves the Node from the given Handle
 * @property isNodeInInbox Checks whether the Node is a Backup Node or not
 * @property savedStateHandle The Saved State Handle to retrieve the parameters sent
 */
@HiltViewModel
class VersionsBottomSheetDialogViewModel @Inject constructor(
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val getNodeByHandle: GetNodeByHandle,
    private val isNodeInInbox: IsNodeInInbox,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(VersionsBottomSheetDialogState())

    /**
     * UI State class for [mega.privacy.android.app.modalbottomsheet.VersionsBottomSheetDialogFragment]
     */
    val state: StateFlow<VersionsBottomSheetDialogState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val nodeHandle: Long? =
                savedStateHandle[VersionsBottomSheetDialogFragment.PARAM_NODE_HANDLE]
            val selectedNodePosition: Int =
                savedStateHandle[VersionsBottomSheetDialogFragment.PARAM_SELECTED_POSITION] ?: -1

            nodeHandle?.let { nonNullHandle ->
                // Async-Await is used here in order to prevent the hidden options from being
                // blackened and taking up space in the popup Dialog
                val isNodeInBackups = async { isNodeInInbox(nodeHandle) }
                val node = async { getNodeByHandle(nonNullHandle) }
                val accessPermission = async { getNodeAccessPermission(NodeId(nonNullHandle)) }

                val isNodeTheCurrentVersion = selectedNodePosition == 0
                val isDeleteAccessPermissionEligible =
                    isDeleteAccessPermissionEligible(accessPermission.await())

                val canDeleteVersion = if (isNodeInBackups.await()) {
                    !isNodeTheCurrentVersion && isDeleteAccessPermissionEligible
                } else isDeleteAccessPermissionEligible
                val canRevertVersion = if (isNodeInBackups.await()) {
                    false
                } else !isNodeTheCurrentVersion && isRevertAccessPermissionEligible(
                    accessPermission.await()
                )

                _state.update {
                    it.copy(
                        canDeleteVersion = canDeleteVersion,
                        canRevertVersion = canRevertVersion,
                        node = node.await(),
                    )
                }
            }
        }
    }

    /**
     * Checks whether the current Node Access Permission meets any of the listed
     * [AccessPermission] for deleting a Version
     */
    private fun isDeleteAccessPermissionEligible(accessPermission: AccessPermission?) =
        accessPermission in listOf(AccessPermission.FULL, AccessPermission.OWNER)

    /**
     * Checks whether the current Node Access Permission meets any of the listed
     * [AccessPermission] for reverting a Version
     */
    private fun isRevertAccessPermissionEligible(accessPermission: AccessPermission?) =
        accessPermission in listOf(
            AccessPermission.READWRITE,
            AccessPermission.FULL,
            AccessPermission.OWNER,
        )
}