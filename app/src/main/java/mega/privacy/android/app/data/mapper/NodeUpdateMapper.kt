package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import nz.mega.sdk.MegaNode

/**
 * Mapper to convert list of updated nodes from sdk into a [NodeUpdate] entity
 */
typealias NodeUpdateMapper = (@JvmSuppressWildcards List<@JvmSuppressWildcards MegaNode>) -> NodeUpdate


/**
 * Maps from mega node list to NodeUpdate
 *
 * @param nodeList
 */
internal fun mapMegaNodeListToNodeUpdate(nodeList: List<MegaNode>) = NodeUpdate(
    nodeList.groupBy { node -> NodeId(node.handle) }
        .mapValues { (_, nodes) ->
            nodes.map { i -> fromMegaNodeChangeFlags(i.changes) }.flatten()
        }
)

private fun fromMegaNodeChangeFlags(changeFlags: Int) = nodeChangesMap.filter {
    it.key and changeFlags != 0
}.values.toList()

private val nodeChangesMap = mapOf(
    MegaNode.CHANGE_TYPE_REMOVED to NodeChanges.Remove,
    MegaNode.CHANGE_TYPE_ATTRIBUTES to NodeChanges.Attributes,
    MegaNode.CHANGE_TYPE_OWNER to NodeChanges.Owner,
    MegaNode.CHANGE_TYPE_TIMESTAMP to NodeChanges.Timestamp,
    MegaNode.CHANGE_TYPE_FILE_ATTRIBUTES to NodeChanges.File_attributes,
    MegaNode.CHANGE_TYPE_INSHARE to NodeChanges.Inshare,
    MegaNode.CHANGE_TYPE_OUTSHARE to NodeChanges.Outshare,
    MegaNode.CHANGE_TYPE_PARENT to NodeChanges.Parent,
    MegaNode.CHANGE_TYPE_PENDINGSHARE to NodeChanges.Pendingshare,
    MegaNode.CHANGE_TYPE_PUBLIC_LINK to NodeChanges.Public_link,
    MegaNode.CHANGE_TYPE_NEW to NodeChanges.New,
    MegaNode.CHANGE_TYPE_NAME to NodeChanges.Name,
    MegaNode.CHANGE_TYPE_FAVOURITE to NodeChanges.Favourite
)


