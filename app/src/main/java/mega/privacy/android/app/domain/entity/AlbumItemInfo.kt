package mega.privacy.android.app.domain.entity

import nz.mega.sdk.MegaNode

/**
 * The entity for AlbumItem info
 * @param node current favourite node
 */
data class AlbumItemInfo(
    val node: MegaNode
)