package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.FavouriteInfo
import nz.mega.sdk.MegaNode

/**
 * Mapper for [FavouriteFolderInfo]
 */
typealias FavouriteFolderInfoMapper = (@JvmSuppressWildcards MegaNode, @JvmSuppressWildcards List<@JvmSuppressWildcards FavouriteInfo>, @JvmSuppressWildcards Long) -> @JvmSuppressWildcards FavouriteFolderInfo

/**
 * To favourite folder info
 *
 * @param node
 * @param children
 * @param id
 */
internal fun toFavouriteFolderInfo(
    node: MegaNode,
    children: List<FavouriteInfo>,
    id: Long,
) = FavouriteFolderInfo(
    children = children,
    name = node.name,
    currentHandle = id,
    parentHandle = node.parentHandle,
)