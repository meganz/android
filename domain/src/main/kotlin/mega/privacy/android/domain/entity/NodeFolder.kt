package mega.privacy.android.domain.entity


/**
 * Node folder
 *
 * @property id
 * @property name
 * @property parentId
 * @property base64Id
 * @property label
 * @property hasVersion
 * @property numChildFolders
 * @property numChildFiles
 * @property isFavourite
 * @property isExported
 * @property isTakenDown
 * @property isInRubbishBin
 * @property isIncomingShare
 * @property isShared
 * @property isPendingShare
 * @property device
 */
data class NodeFolder(
    override val id: Long,
    override val name: String,
    override val parentId: Long,
    override val base64Id: String,
    override val label: Int,
    override val hasVersion: Boolean,
    val numChildFolders: Int,
    val numChildFiles: Int,
    override val isFavourite: Boolean,
    override val isExported: Boolean,
    override val isTakenDown: Boolean,
    override val isInRubbishBin: Boolean,
    override val isIncomingShare: Boolean,
    override val isShared: Boolean,
    override val isPendingShare: Boolean,
    override val device: String?,
) : NodeInfo, Folder