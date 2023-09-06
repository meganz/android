package mega.privacy.android.app.presentation.mapper

import android.view.MenuItem
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.CheckAccessErrorExtended
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetRubbishBinFolderUseCase
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaShare
import javax.inject.Inject

/**
 * Gets options for toolbar Mapper
 */
class GetOptionsForToolbarMapper @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val checkAccessErrorExtended: CheckAccessErrorExtended,
    private val checkNodeCanBeMovedToTargetNode: CheckNodeCanBeMovedToTargetNode,
    private val rubbishBinFolder: GetRubbishBinFolderUseCase,
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
                    if (node.isTakenDown.not()) {
                        if (checkAccessErrorExtended(
                                it,
                                MegaShare.ACCESS_OWNER
                            ).errorCode == MegaError.API_OK
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
                    if (checkAccessErrorExtended(
                            it,
                            MegaShare.ACCESS_FULL
                        ).errorCode == MegaError.API_OK
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

        selectedNodeHandleList.forEach { handle ->
            getNodeByIdUseCase(NodeId(handle))?.let { node ->
                getNodeByHandle(handle)?.let { megaNode ->
                    if (node.isTakenDown) {
                        showDispute = true
                        showShareOut = false
                        showCopy = false
                        showDownload = false
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
                    val rubbishBinNode = rubbishBinFolder()
                    if (rubbishBinNode != null && !checkNodeCanBeMovedToTargetNode(
                            NodeId(megaNode.handle),
                            NodeId(rubbishBinNode.handle)
                        )
                    ) {
                        showTrash = false
                    }
                    if (node.isTakenDown || node is FileNode || isOutShare(node).not()) {
                        showRemoveShare = false
                    }
                }

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
                if ((checkAccessErrorExtended(
                        megaNode,
                        MegaShare.ACCESS_OWNER
                    ).errorCode != MegaError.API_OK) || megaNode.isTakenDown
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