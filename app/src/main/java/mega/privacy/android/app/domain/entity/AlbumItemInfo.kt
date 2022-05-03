package mega.privacy.android.app.domain.entity

/**
 * The entity for AlbumItem info
 * @param handle current favourite node handle
 */
data class AlbumItemInfo(
    val handle:Long,
    val base64Handle:String,
    val modifiedTime: Long
)