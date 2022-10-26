package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.NodeInfo
import nz.mega.sdk.MegaNode

/**
 * Mapper for [FavouriteFolderInfo]
 */
typealias FavouriteFolderInfoMapper = (@JvmSuppressWildcards MegaNode, @JvmSuppressWildcards List<@JvmSuppressWildcards NodeInfo>, @JvmSuppressWildcards Long) -> @JvmSuppressWildcards FavouriteFolderInfo

/**
 * To favourite folder info
 *
 * @param node
 * @param children
 * @param id
 */
internal fun toFavouriteFolderInfo(
    node: MegaNode,
    children: List<NodeInfo>,
    id: Long,
) = FavouriteFolderInfo(
    children = children,
    name = node.name,
    currentHandle = id,
    parentHandle = node.parentHandle,
)