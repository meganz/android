package mega.privacy.android.app.domain.entity

import java.io.File

sealed interface Album {
    val thumbnail: File?
    val itemCount: Int

    data class FavouriteAlbum(override val thumbnail: File?, override val itemCount: Int):Album
}