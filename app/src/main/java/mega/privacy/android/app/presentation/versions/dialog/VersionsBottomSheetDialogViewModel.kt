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
import mega.privacy.android.domain.usecase.node.IsNodeInInboxUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import javax.inject.Inject

/**
 * ViewModel class for [mega.privacy.android.app.modalbottomsheet.VersionsBottomSheetDialogFragment]
 *
 * @property getNodeAccessPermission Retrieves the Node Access Permission
 * @property getNodeByHandle Retrieves the Node from the given Handle
 * @property isNodeInInboxUseCase Checks whether the Node is a Backup Node or not
 * @property savedStateHandle The Saved State Handle to retrieve the parameters sent
 */
@HiltViewModel
class VersionsBottomSheetDialogViewModel @Inject constructor(
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val getNodeByHandle: GetNodeByHandle,
    private val isNodeInInboxUseCase: IsNodeInInboxUseCase,
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
            val versionsCount: Int =
                savedStateHandle[VersionsBottomSheetDialogFragment.PARAM_VERSIONS_COUNT] ?: 0

            nodeHandle?.let { nonNullHandle ->
                // Async-Await is used here in order to prevent the hidden options from being
                // blackened and taking up space in the popup Dialog
                val isNodeInBackups = async { isNodeInInboxUseCase(nodeHandle) }
                val node = async { getNodeByHandle(nonNullHandle) }
                val accessPermission = async { getNodeAccessPermission(NodeId(nonNullHandle)) }

                val isNodeTheCurrentVersion = selectedNodePosition == 0
                val isDeleteAccessPermissionEligible =
                    isDeleteAccessPermissionEligible(accessPermission.await())
                val canDeleteVersion = canDeleteVersion(
                    isDeleteAccessPermissionEligible = isDeleteAccessPermissionEligible,
                    isNodeInBackups = isNodeInBackups.await(),
                    isNodeTheCurrentVersion = isNodeTheCurrentVersion,
                    versionsCount = versionsCount,
                )
                val canRevertVersion = canRevertVersion(
                    isNodeInBackups = isNodeInBackups.await(),
                    isNodeTheCurrentVersion = isNodeTheCurrentVersion,
                    isRevertAccessPermissionEligible = isRevertAccessPermissionEligible(
                        accessPermission.await(),
                    ),
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
     * Checks whether this Version can be deleted or not
     *
     * @param isDeleteAccessPermissionEligible true if the current Node Access Permission meets any
     * of the listed [AccessPermission] for deleting a Version, and false if otherwise
     * @param isNodeInBackups true if the Node exists in Backups, and false if otherwise
     * @param isNodeTheCurrentVersion true if this is the Current Version, and false if otherwise
     * @param versionsCount The current number of Versions in the file
     *
     * @return true if this Version can be deleted, and false if otherwise
     */
    private fun canDeleteVersion(
        isDeleteAccessPermissionEligible: Boolean,
        isNodeInBackups: Boolean,
        isNodeTheCurrentVersion: Boolean,
        versionsCount: Int,
    ) = if (isNodeInBackups) {
        isNodeTheCurrentVersion.not() && isDeleteAccessPermissionEligible
    } else {
        if (versionsCount > 1) isDeleteAccessPermissionEligible
        else false
    }

    /**
     * Checks whether this Version can be reverted or not
     *
     * @param isNodeInBackups true if the Node exists in Backups, and false if otherwise
     * @param isNodeTheCurrentVersion true if this is the Current Version, and false if otherwise
     * @param isRevertAccessPermissionEligible true if the current Node Access Permission meets any
     * of the listed [AccessPermission] for reverting a Version, and false if otherwise
     *
     * @return true if this Version can be reverted, and false if otherwise
     */
    private fun canRevertVersion(
        isNodeInBackups: Boolean,
        isNodeTheCurrentVersion: Boolean,
        isRevertAccessPermissionEligible: Boolean,
    ) = if (isNodeInBackups) {
        false
    } else {
        isNodeTheCurrentVersion.not() && isRevertAccessPermissionEligible
    }

    /**
     * Checks whether the current Node Access Permission meets any of the listed
     * [AccessPermission] for deleting a Version
     *
     * @return true if the Access Permission makes the Version eligible to be deleted, and false
     * if otherwise
     */
    private fun isDeleteAccessPermissionEligible(accessPermission: AccessPermission?) =
        accessPermission in listOf(AccessPermission.FULL, AccessPermission.OWNER)

    /**
     * Checks whether the current Node Access Permission meets any of the listed
     * [AccessPermission] for reverting a Version
     *
     * @return true if the Access Permission makes the Version eligible to be reverted, and false
     * if otherwise
     */
    private fun isRevertAccessPermissionEligible(accessPermission: AccessPermission?) =
        accessPermission in listOf(
            AccessPermission.READWRITE,
            AccessPermission.FULL,
            AccessPermission.OWNER,
        )
}