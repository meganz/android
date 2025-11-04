package mega.privacy.android.feature.photos.model

import androidx.annotation.DrawableRes

data class PhotoNodeUiState(
    val photo: PhotoUiState,
    val isSensitive: Boolean,
    val isSelected: Boolean,
    @DrawableRes val defaultIcon: Int,
)
