package mega.privacy.android.domain.entity.node

import mega.privacy.android.domain.entity.FileTypeInfo

data class DefaultFileNode(
    override val id: NodeId,
    override val name: String,
    override val parentId: NodeId,
    override val base64Id: String,
    override val size: Long,
    override val label: Int,
    override val modificationTime: Long,
    override val hasVersion: Boolean,
    override val type: FileTypeInfo,
    override val thumbnailPath: String? = null,
    override val isFavourite: Boolean,
    override val isExported: Boolean,
    override val isTakenDown: Boolean,
) : FileNode