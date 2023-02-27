package mega.privacy.android.domain.entity.photos

data class AlbumPhotosRemovingProgress(
    val isProgressing: Boolean,
    val totalRemovedPhotos: Int,
)