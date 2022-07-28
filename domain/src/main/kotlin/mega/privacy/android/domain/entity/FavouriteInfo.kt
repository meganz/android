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
     * Size
     */
    val size: Long

    /**
     * Label
     */
    val label: Int

    /**
     * Modification time
     */
    val modificationTime: Long

    /**
     * Has version
     */
    val hasVersion: Boolean

    /**
     * Num child folders
     */
    val numChildFolders: Int

    /**
     * Num child files
     */
    val numChildFiles: Int

    /**
     * Is image
     */
    val isImage: Boolean

    /**
     * Is video
     */
    val isVideo: Boolean

    /**
     * Is folder
     */
    val isFolder: Boolean

    /**
     * Thumbnail path
     */
    val thumbnailPath: String?

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
 * @property numChildFolders
 * @property numChildFiles
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
    override val size: Long,
    override val label: Int,
    override val modificationTime: Long,
    override val hasVersion: Boolean,
    override val numChildFolders: Int,
    override val numChildFiles: Int,
    val type: FileTypeInfo,
    override val thumbnailPath: String? = null,
    override val isFavourite: Boolean,
    override val isExported: Boolean,
    override val isTakenDown: Boolean,
) : FavouriteInfo {
    override val isFolder: Boolean = false
    override val isImage: Boolean
        get() = type is ImageFileTypeInfo
    override val isVideo: Boolean
        get() = type is VideoFileTypeInfo
}

/**
 * Favourite folder
 *
 * @property id
 * @property name
 * @property parentId
 * @property base64Id
 * @property size
 * @property label
 * @property modificationTime
 * @property hasVersion
 * @property numChildFolders
 * @property numChildFiles
 * @property thumbnailPath
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
    override val size: Long,
    override val label: Int,
    override val modificationTime: Long,
    override val hasVersion: Boolean,
    override val numChildFolders: Int,
    override val numChildFiles: Int,
    override val thumbnailPath: String? = null,
    override val isFavourite: Boolean,
    override val isExported: Boolean,
    override val isTakenDown: Boolean,
) : FavouriteInfo {
    override val isFolder: Boolean = true
    override val isImage: Boolean = false
    override val isVideo: Boolean = false
}