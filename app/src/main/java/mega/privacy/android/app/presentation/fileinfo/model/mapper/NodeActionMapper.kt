package mega.privacy.android.app.presentation.fileinfo.model.mapper

import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.domain.entity.node.NodeAction
import javax.inject.Inject

/**
 * Maps NodeAction to FileInfoMenuAction
 */
class NodeActionMapper @Inject constructor() {
    internal operator fun invoke(nodeAction: NodeAction) = when (nodeAction) {
        NodeAction.Download -> FileInfoMenuAction.Download
        NodeAction.ShareFolder -> FileInfoMenuAction.ShareFolder
        NodeAction.GetLink -> FileInfoMenuAction.GetLink
        NodeAction.SendToChat -> FileInfoMenuAction.SendToChat
        NodeAction.ManageLink -> FileInfoMenuAction.ManageLink
        NodeAction.RemoveLink -> FileInfoMenuAction.RemoveLink
        NodeAction.DisputeTakedown -> FileInfoMenuAction.DisputeTakedown
        NodeAction.Rename -> FileInfoMenuAction.Rename
        NodeAction.Move -> FileInfoMenuAction.Move
        NodeAction.Copy -> FileInfoMenuAction.Copy
        NodeAction.MoveToRubbishBin -> FileInfoMenuAction.MoveToRubbishBin
        NodeAction.Leave -> FileInfoMenuAction.Leave
        NodeAction.Delete -> FileInfoMenuAction.Delete
    }
}