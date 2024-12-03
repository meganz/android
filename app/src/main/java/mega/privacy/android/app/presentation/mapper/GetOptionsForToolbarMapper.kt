package mega.privacy.android.app.presentation.mapper

import android.view.MenuItem
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.NodeAccessPermissionCheckUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinFolderUseCase
import javax.inject.Inject

/**
 * Gets options for toolbar Mapper
 */
class GetOptionsForToolbarMapper @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val nodeAccessPermissionCheckUseCase: NodeAccessPermissionCheckUseCase,
    private val checkNodeCanBeMovedToTargetNode: CheckNodeCanBeMovedToTargetNode,
    private val getRubbishBinFolderUseCase: GetRubbishBinFolderUseCase,
) {

    /**
     * Invoke
     * @param selectedNodeHandleList list of selected node handle
     * @param totalNodes total nodes count
     * @return [CloudStorageOptionControlUtil.Control]
     */
    suspend operator fun invoke(
        selectedNodeHandleList: List<Long>,
        totalNodes: Int,
    ): CloudStorageOptionControlUtil.Control {
        val control = CloudStorageOptionControlUtil.Control()
        if (selectedNodeHandleList.size == 1) {
            val megaNode = getNodeByHandle(selectedNodeHandleList[0])
            getNodeByIdUseCase(NodeId(selectedNodeHandleList[0]))?.let { node ->
                megaNode?.let {
                    val megaNodeId = NodeId(it.handle)
                    if (node.isTakenDown.not()) {
                        if (nodeAccessPermissionCheckUseCase(
                                nodeId = megaNodeId,
                                level = AccessPermission.OWNER,
                            )
                        ) {
                            if (it.isExported) {
                                control.manageLink().setVisible(true).showAsAction =
                                    MenuItem.SHOW_AS_ACTION_ALWAYS
                                control.removeLink().isVisible = true
                            } else {
                                control.link.setVisible(true).showAsAction =
                                    MenuItem.SHOW_AS_ACTION_ALWAYS
                            }
                        }
                    }
                    if (nodeAccessPermissionCheckUseCase(
                            nodeId = megaNodeId,
                            level = AccessPermission.FULL,
                        )
                    ) {
                        control.rename().isVisible = true
                    }
                }
            }
        } else if (allHaveOwnerAccessAndNotTakenDown(selectedNodeHandleList)) {
            control.link.apply {
                isVisible = true
                showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            }
        }

        var showSendToChat = true
        var showShareFolder = true
        var showTrash = true
        var showRemoveShare = true
        var showShareOut = true
        var showCopy = true
        var showDownload = true
        var mediaCounter = 0
        var showDispute = false
        var showLeaveShare = false

        selectedNodeHandleList.forEach { handle ->
            getNodeByIdUseCase(NodeId(handle))?.let { node ->
                getNodeByHandle(handle)?.let { megaNode ->
                    if (node.isTakenDown) {
                        showDispute = true
                        showShareOut = false
                        showCopy = false
                        showDownload = false
                    }
                    if (node.isIncomingShare) {
                        showLeaveShare = true
                    }
                    if (node is FileNode) {
                        val nodeMime = MimeTypeList.typeForName(
                            node.name
                        )
                        if (nodeMime.isImage || nodeMime.isVideo) {
                            mediaCounter++
                        }
                    } else if (node.isTakenDown || node is FolderNode) {
                        showSendToChat = false
                    }
                    if (node.isTakenDown || node is FileNode || isOutShare(node) && selectedNodeHandleList.size > 1) {
                        showShareFolder = false
                    }
                    val rubbishBinNode = getRubbishBinFolderUseCase()
                    if (rubbishBinNode != null && !checkNodeCanBeMovedToTargetNode(
                            NodeId(megaNode.handle),
                            NodeId(rubbishBinNode.id.longValue)
                        )
                    ) {
                        showTrash = false
                    }
                    if (node.isTakenDown || node is FileNode || isOutShare(node).not()) {
                        showRemoveShare = false
                    }
                }

                control.hide().setVisible(false).showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
                control.unhide().setVisible(false).showAsAction = MenuItem.SHOW_AS_ACTION_NEVER

                if (showSendToChat) {
                    control.sendToChat().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                if (showShareFolder) {
                    control.shareFolder().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                if (showRemoveShare) {
                    control.removeShare().isVisible = true
                }
                control.trash().isVisible = showTrash
                if (showLeaveShare) {
                    control.leaveShare().isVisible = true
                    if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                        control.leaveShare().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                    }
                }
                if (showShareOut) {
                    control.shareOut().isVisible = true
                    if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                        control.shareOut().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                    }
                }
                control.move().isVisible = true
                if (selectedNodeHandleList.size > 1
                    && control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT
                ) {
                    control.move().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                if (showCopy) {
                    control.copy().isVisible = true
                    if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                        control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                    }
                }
                if (showDispute) {
                    control.trash().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                    control.move().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                    if (selectedNodeHandleList.size == 1) {
                        control.disputeTakedown().isVisible = true
                        control.disputeTakedown().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                        control.rename().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                    }
                }
                if (!showDownload) {
                    control.saveToDevice().isVisible = false
                }
                control.selectAll().isVisible = selectedNodeHandleList.size < totalNodes
            }
        }
        return control
    }

    /**
     * Checks if for all nodes, access has been taken down
     * @param selectedNodeHandleList
     * @return true/ false
     */
    private suspend fun allHaveOwnerAccessAndNotTakenDown(selectedNodeHandleList: List<Long>): Boolean {
        selectedNodeHandleList.forEach {
            getNodeByHandle(it)?.let { megaNode ->
                if (nodeAccessPermissionCheckUseCase(
                        nodeId = NodeId(megaNode.handle),
                        level = AccessPermission.OWNER,
                    ) || megaNode.isTakenDown
                ) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Checks if current node has any out shares
     * @param node
     * @return true/false
     */
    private fun isOutShare(node: TypedNode): Boolean {
        return if (node is FolderNode) {
            node.isPendingShare || node.isShared
        } else {
            false
        }
    }
}