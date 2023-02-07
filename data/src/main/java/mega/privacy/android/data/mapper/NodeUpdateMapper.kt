package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.node.NodeChanges
import nz.mega.sdk.MegaNode

/**
 * Mapper to convert updated nodes from sdk into a List of [NodeChanges]
 */
typealias NodeUpdateMapper = (@JvmSuppressWildcards MegaNode) -> @JvmSuppressWildcards List<@JvmSuppressWildcards NodeChanges>

/**
 * Maps from [MegaNode]  to list of [NodeChanges]
 *
 * @param nodeList
 */
internal fun mapMegaNodeListToNodeUpdate(node: MegaNode) = fromMegaNodeChangeFlags(node.changes)

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


