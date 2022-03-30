package mega.privacy.android.app.domain.entity

import nz.mega.sdk.MegaNode

/**
 * The entity for favourite info
 * @param node current favourite node
 * @param hasVersion whether current favourite item has version
 * @param numChildFolders child folders number
 * @param numChildFiles child files number
 */
data class FavouriteInfo(
    val node: MegaNode,
    val hasVersion: Boolean,
    val numChildFolders: Int,
    val numChildFiles: Int
)