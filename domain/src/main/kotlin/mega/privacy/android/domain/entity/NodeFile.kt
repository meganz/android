package mega.privacy.android.domain.entity

/**
 * Node file
 *
 * @property id
 * @property name
 * @property parentId
 * @property base64Id
 * @property size
 * @property label
 * @property modificationTime
 * @property hasVersion
 * @property type
 * @property thumbnailPath
 * @property isFavourite
 * @property isExported
 * @property isTakenDown
 */
data class NodeFile(
    override val id: Long,
    override val name: String,
    override val parentId: Long,
    override val base64Id: String,
    val size: Long,
    override val label: Int,
    val modificationTime: Long,
    override val hasVersion: Boolean,
    val type: FileTypeInfo,
    val thumbnailPath: String? = null,
    override val isFavourite: Boolean,
    override val isExported: Boolean,
    override val isTakenDown: Boolean,
) : NodeInfo