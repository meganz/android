package mega.privacy.android.app.presentation.extensions

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.entity.AlbumItemInfo
import mega.privacy.android.app.presentation.photos.model.ALBUM_ID_FAV
import mega.privacy.android.app.presentation.photos.model.AlbumCoverItem
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import java.io.File

/**
 * Map album cover list from album item info list
 */
fun List<AlbumItemInfo>.toAlbumCoverList(): List<AlbumCoverItem> {
    val newList = ArrayList<AlbumCoverItem>()
    val newAlbumCoverItem = createEmptyFavAlbum()
    if (this.isNotEmpty()) {
        newAlbumCoverItem.handle = this[0].handle
        newAlbumCoverItem.nodeBase64Handle = this[0].base64Handle
        newAlbumCoverItem.itemCount = this.size
    }
    newList.add(newAlbumCoverItem)
    return newList
}

/**
 * Create an empty favorite album with it's unique id.
 */
private fun createEmptyFavAlbum() =
    AlbumCoverItem(albumId = ALBUM_ID_FAV)

/**
 * Map album title by album id
 */
fun AlbumCoverItem.mapAlbumTitle(context: Context): String {
    return if (this.albumId == ALBUM_ID_FAV) {
        context.getString(R.string.title_favourites_album)
    } else {
        ""
    }
}


fun AlbumCoverItem.getThumbnailPath(context: Context): String? {
    if (this.nodeBase64Handle.isNullOrBlank())
        return null
    val thumbnailFolder = File(context.cacheDir, CacheFolderManager.THUMBNAIL_FOLDER)
    return "file://" + File(
        thumbnailFolder,
        nodeBase64Handle.plus(FileUtil.JPG_EXTENSION)
    ).absolutePath
}

