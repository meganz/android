package mega.privacy.android.app.data.extensions

import mega.privacy.android.app.domain.entity.FavouriteInfo
import nz.mega.sdk.MegaNode

/**
 * Convert MegaNode to FavouriteInfo
 * @param hasVersion the node if has version
 * @param numChildFolders child folders number
 * @param numChildFiles child files number
 * @return FavouriteInfo
 */
fun MegaNode.toFavouriteInfo(hasVersion: Boolean, numChildFolders: Int, numChildFiles: Int): FavouriteInfo {
    return FavouriteInfo(
        node = this,
        hasVersion = hasVersion,
        numChildFolders = numChildFolders,
        numChildFiles = numChildFiles
    )
}