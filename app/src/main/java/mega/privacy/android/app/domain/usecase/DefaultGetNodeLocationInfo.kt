package mega.privacy.android.app.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import javax.inject.Inject

/**
 * Default implementation for [GetNodeLocationInfo].
 *
 * Resolves the human readable location of a [TypedNode] by walking the node's ancestry
 * through [NodeRepository] (no direct SDK access).
 */
class DefaultGetNodeLocationInfo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nodeRepository: NodeRepository,
) : GetNodeLocationInfo {

    override suspend fun invoke(typedNode: TypedNode): LocationInfo? {
        val nodeId = typedNode.id
        val fromIncomingShare = nodeRepository.getOwnerIdFromInShare(nodeId, true) != null

        val parent = nodeRepository.getParentNode(nodeId)
        val topAncestor = nodeRepository.getRootParentNode(nodeId) ?: return null

        val rootNodeId = nodeRepository.getRootNode()?.id
        val rubbishNodeId = nodeRepository.getRubbishNode()?.id
        val backupsNodeId = nodeRepository.getBackupsNode()?.id

        val inCloudDrive = topAncestor.id == rootNodeId || topAncestor.id == rubbishNodeId
        val inBackups = topAncestor.id == backupsNodeId

        val location = when {
            fromIncomingShare -> {
                if (parent != null) {
                    val parentNodeName = if (parent.isNodeKeyDecrypted) {
                        parent.name
                    } else {
                        context.getString(R.string.shared_items_verify_credentials_undecrypted_folder)
                    }
                    context.getString(
                        R.string.location_label, parentNodeName,
                        context.getString(sharedR.string.shares_screen_incoming_shares_tab_title)
                    )
                } else {
                    context.getString(sharedR.string.shares_screen_incoming_shares_tab_title)
                }
            }

            parent == null -> {
                context.getString(sharedR.string.shares_screen_incoming_shares_tab_title)
            }

            inCloudDrive -> {
                if (topAncestor.id == parent.id) {
                    getTranslatedNameForParentNode(topAncestor, rootNodeId, rubbishNodeId, backupsNodeId)
                } else {
                    context.getString(
                        R.string.location_label, parent.name,
                        getTranslatedNameForParentNode(topAncestor, rootNodeId, rubbishNodeId, backupsNodeId)
                    )
                }
            }

            inBackups -> {
                if (parent.id == backupsNodeId) {
                    // If the Node's parent is the My Backups node,
                    // only display the name of the Root Node
                    getTranslatedNameForParentNode(topAncestor, rootNodeId, rubbishNodeId, backupsNodeId)
                } else {
                    // Otherwise, include the names of both the Parent and Root Nodes
                    context.getString(
                        R.string.location_label, parent.name,
                        getTranslatedNameForParentNode(topAncestor, rootNodeId, rubbishNodeId, backupsNodeId)
                    )
                }
            }

            else -> {
                context.getString(
                    R.string.location_label, parent.name,
                    context.getString(sharedR.string.shares_screen_incoming_shares_tab_title)
                )
            }
        }

        val fragmentHandle = when {
            fromIncomingShare || parent == null -> INVALID_HANDLE
            inCloudDrive -> topAncestor.id.longValue
            else -> INVALID_HANDLE
        }

        return LocationInfo(
            location = location,
            parentHandle = parent?.id?.longValue ?: INVALID_HANDLE,
            fragmentHandle = fragmentHandle
        )
    }

    private fun getTranslatedNameForParentNode(
        parent: Node,
        rootNodeId: NodeId?,
        rubbishNodeId: NodeId?,
        backupsNodeId: NodeId?,
    ): String = when (parent.id) {
        rootNodeId -> context.getString(R.string.section_cloud_drive)
        rubbishNodeId -> context.getString(sharedR.string.general_section_rubbish_bin)
        backupsNodeId -> context.getString(R.string.home_side_menu_backups_title)
        else -> parent.name
    }
}
