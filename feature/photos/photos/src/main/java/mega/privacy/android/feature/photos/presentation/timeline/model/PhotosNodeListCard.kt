package mega.privacy.android.feature.photos.presentation.timeline.model

import mega.privacy.android.feature.photos.model.PhotoUiState

data class PhotosNodeListCard(
    val period: PhotosNodeListCardPeriod,
    val key: Long,
    val id: Long,
    val day: Int,
    val month: Int,
    val year: Int,
    val formattedDate: String,
    val thumbnailFilePath: String?,
    val previewFilePath: String?,
    val extension: String,
    val isSensitive: Boolean,
    val count: Int,
)

enum class PhotosNodeListCardPeriod {
    Day, Month, Year
}

data class PhotoNodeListCardItem(
    val photo: PhotoUiState,
    val isMarkedSensitive: Boolean,
)

enum class PhotosNodeListCardCount(val portrait: Int, val landscape: Int) {
    Grid(1, 2)
}
