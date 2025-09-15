package mega.privacy.android.app.presentation.validator.toolbaractions

import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNode
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNodeType.File
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNodeType.Folder
import mega.privacy.android.app.presentation.validator.toolbaractions.model.ToolbarActionsRequest
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.ToolbarActionsModifier
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil.MAX_ACTION_COUNT
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

class ToolbarActionsValidator @Inject constructor(
    private val modifiers: Set<@JvmSuppressWildcards ToolbarActionsModifier>,
) {

    operator fun invoke(request: ToolbarActionsRequest): CloudStorageOptionControlUtil.Control {
        val control = CloudStorageOptionControlUtil.Control()
        if (request.selectedNodes.size == 1) {
            val node = request.selectedNodes.first()
            if (node.isTakenDown.not() && node.accessPermissions.contains(AccessPermission.OWNER)) {
                if (node.isExported) {
                    control.manageLink().setVisible(true).showAsAction = SHOW_AS_ACTION_ALWAYS
                    control.removeLink().isVisible = true
                } else {
                    control.link.setVisible(true).showAsAction = SHOW_AS_ACTION_ALWAYS
                }
            }
            if (node.accessPermissions.contains(AccessPermission.FULL)) {
                control.rename().isVisible = true
            }
        } else if (allHaveOwnerAccessAndNotTakenDown(selectedNodes = request.selectedNodes)) {
            control.link.apply {
                isVisible = true
                showAsAction = SHOW_AS_ACTION_ALWAYS
            }
        }

        // Set flags
        val flags = getValidationFlags(selectedNodes = request.selectedNodes)

        // Update control by flags
        val updatedControlByFlags = getDefaultControlBasedOnValidationFlags(
            control = control,
            validationFlags = flags,
            totalSelectedNodes = request.selectedNodes.size,
            totalNodes = request.totalNodes
        )

        // Update control based on modifiers
        val modifier = modifiers.find { it.canHandle(item = request.modifierItem) }
        modifier?.modify(
            control = updatedControlByFlags,
            item = request.modifierItem
        )

        return control
    }

    private fun allHaveOwnerAccessAndNotTakenDown(selectedNodes: List<SelectedNode>): Boolean {
        return selectedNodes.find { node ->
            !node.accessPermissions.contains(AccessPermission.OWNER) || node.isTakenDown
        } == null
    }

    private fun getValidationFlags(selectedNodes: List<SelectedNode>): ValidationFlags {
        val validationFlags = ValidationFlags()
        selectedNodes.forEach { node ->
            if (node.isTakenDown) {
                validationFlags.showDispute = true
                validationFlags.showShareOut = false
                validationFlags.showCopy = false
                validationFlags.showDownload = false
            }
            if (node.isIncomingShare) {
                validationFlags.showLeaveShare = true
            }
            if (node.type !is File && (node.isTakenDown || node.type is Folder)) {
                validationFlags.showSendToChat = false
            }
            if (node.isTakenDown || node.type is File || isOutShare(node) && selectedNodes.size > 1) {
                validationFlags.showShareFolder = false
            }
            if (!node.canBeMovedToRubbishBin) {
                validationFlags.showTrash = false
            }
            if (node.isTakenDown || node.type is File || isOutShare(node).not()) {
                validationFlags.showRemoveShare = false
            }
        }
        return validationFlags
    }

    private fun isOutShare(node: SelectedNode): Boolean {
        return if (node.type is Folder) {
            node.type.isPendingShare || node.type.isShared
        } else {
            false
        }
    }

    private fun getDefaultControlBasedOnValidationFlags(
        control: CloudStorageOptionControlUtil.Control,
        validationFlags: ValidationFlags,
        totalSelectedNodes: Int,
        totalNodes: Int,
    ): CloudStorageOptionControlUtil.Control {
        control.hide().setVisible(false).showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
        control.unhide().setVisible(false).showAsAction = MenuItem.SHOW_AS_ACTION_NEVER

        if (validationFlags.showSendToChat) {
            control.sendToChat().setVisible(true).showAsAction = SHOW_AS_ACTION_ALWAYS
        }

        if (validationFlags.showShareFolder) {
            control.shareFolder().setVisible(true).showAsAction = SHOW_AS_ACTION_ALWAYS
        }

        if (validationFlags.showRemoveShare) {
            control.removeShare().isVisible = true
        }

        control.trash().isVisible = validationFlags.showTrash

        if (validationFlags.showLeaveShare) {
            control.leaveShare().isVisible = true
            if (control.alwaysActionCount() < MAX_ACTION_COUNT) {
                control.leaveShare().showAsAction = SHOW_AS_ACTION_ALWAYS
            }
        }

        if (validationFlags.showShareOut) {
            control.shareOut().isVisible = true
            if (control.alwaysActionCount() < MAX_ACTION_COUNT) {
                control.shareOut().showAsAction = SHOW_AS_ACTION_ALWAYS
            }
        }

        control.move().isVisible = true
        if (totalSelectedNodes > 1 && control.alwaysActionCount() < MAX_ACTION_COUNT) {
            control.move().showAsAction = SHOW_AS_ACTION_ALWAYS
        }

        if (validationFlags.showCopy) {
            control.copy().isVisible = true
            if (control.alwaysActionCount() < MAX_ACTION_COUNT) {
                control.copy().showAsAction = SHOW_AS_ACTION_ALWAYS
            }
        }

        if (validationFlags.showDispute) {
            control.trash().showAsAction = SHOW_AS_ACTION_ALWAYS
            control.move().showAsAction = SHOW_AS_ACTION_ALWAYS
            if (totalSelectedNodes == 1) {
                control.disputeTakedown().isVisible = true
                control.disputeTakedown().showAsAction = SHOW_AS_ACTION_ALWAYS
                control.rename().showAsAction = SHOW_AS_ACTION_ALWAYS
            }
        }

        if (!validationFlags.showDownload) {
            control.saveToDevice().isVisible = false
        }

        control.selectAll().isVisible = totalSelectedNodes < totalNodes

        return control
    }

    private data class ValidationFlags(
        var showSendToChat: Boolean = true,
        var showShareFolder: Boolean = true,
        var showTrash: Boolean = true,
        var showRemoveShare: Boolean = true,
        var showShareOut: Boolean = true,
        var showCopy: Boolean = true,
        var showDownload: Boolean = true,
        var showDispute: Boolean = false,
        var showLeaveShare: Boolean = false,
    )
}
