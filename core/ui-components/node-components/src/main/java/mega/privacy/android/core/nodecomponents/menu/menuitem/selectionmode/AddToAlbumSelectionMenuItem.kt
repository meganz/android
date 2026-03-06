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
    override val menuAction: AddToAlbumMenuAction,
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        nodeSourceType: NodeSourceType,
    ): Boolean = if (nodeSourceType == NodeSourceType.TIMELINE) {
        // In Timeline, we only load ImageFileTypeInfo and VideoFileTypeInfo, so this menu item should always be displayed.
        true
    } else {
        var allFilesAreImageOrVideo = true
        var allFilesAreVideo = true
        for (node in selectedNodes) {
            if (node !is FileNode) {
                allFilesAreImageOrVideo = false
                allFilesAreVideo = false
                break
            }

            when (node.type) {
                is VideoFileTypeInfo -> Unit
                is ImageFileTypeInfo -> allFilesAreVideo = false
                else -> {
                    allFilesAreImageOrVideo = false
                    allFilesAreVideo = false
                    break
                }
            }
        }
        allFilesAreImageOrVideo && !allFilesAreVideo
    }
}
