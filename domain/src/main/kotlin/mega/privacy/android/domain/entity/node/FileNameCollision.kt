package mega.privacy.android.domain.entity.node

import mega.privacy.android.domain.entity.uri.UriPath

/**
 * File name collision
 *
 * @property path
 */
data class FileNameCollision(
    override val collisionHandle: Long,
    override val name: String,
    override val size: Long,
    override val childFolderCount: Int,
    override val childFileCount: Int,
    override val lastModified: Long,
    override val parentHandle: Long,
    override val isFile: Boolean,
    override val renameName: String? = null,
    val path: UriPath,
) : NameCollision