package mega.privacy.android.app.domain.entity

/**
 * The entity for favourite folder info
 * @param children favourite list
 * @param name current folder name
 * @param currentHandle current folder node handle
 * @param parentHandle parent node handle of current folder node
 */
data class FavouriteFolderInfo(
    val children: List<FavouriteInfo>,
    val name: String,
    val currentHandle: Long,
    val parentHandle: Long
)