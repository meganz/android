package mega.privacy.android.feature.photos.model

import androidx.annotation.DrawableRes

data class PhotoNodeUiState(
    val photo: PhotoUiState,
    val isSensitive: Boolean,
    @DrawableRes val defaultIcon: Int,
)
