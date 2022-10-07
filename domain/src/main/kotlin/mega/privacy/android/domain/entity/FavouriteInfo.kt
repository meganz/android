package mega.privacy.android.domain.entity


/**
 * Favourite info
 */
sealed interface FavouriteInfo {

    /**
     * Id
     */
    val id: Long

    /**
     * Name
     */
    val name: String

    /**
     * Parent id
     */
    val parentId: Long

    /**
     * Base64id
     */
    val base64Id: String

    /**
     * Label
     */
    val label: Int

    /**
     * Has version
     */
    val hasVersion: Boolean

    /**
     * Is favourite
     */
    val isFavourite: Boolean

    /**
     * Is exported
     */
    val isExported: Boolean

    /**
     * Is taken down
     */
    val isTakenDown: Boolean
}

/**
 * Favourite file
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
data class FavouriteFile(
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
) : FavouriteInfo

/**
 * Favourite folder
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
 * @constructor Create empty Favourite folder
 */
data class FavouriteFolder(
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
) : FavouriteInfo