package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.AddToAlbumMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class AddToAlbumSelectionMenuItem @Inject constructor(
    override val menuAction: AddToAlbumMenuAction
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        nodeSourceType: NodeSourceType,
    ): Boolean {
        val allFilesAreImageOrVideo = selectedNodes.all { node ->
            node is FileNode && (node.type is ImageFileTypeInfo || node.type is VideoFileTypeInfo)
        }
        val allFilesAreVideo = selectedNodes.all { node ->
            node is FileNode && node.type is VideoFileTypeInfo
        }
        return allFilesAreImageOrVideo && !allFilesAreVideo
    }
}