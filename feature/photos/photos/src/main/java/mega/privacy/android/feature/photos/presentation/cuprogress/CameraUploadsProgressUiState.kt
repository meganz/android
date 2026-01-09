package mega.privacy.android.feature.photos.presentation.cuprogress

import android.net.Uri
import mega.privacy.android.domain.entity.photos.CameraUploadsTransferType

data class CameraUploadsProgressUiState(
    val isLoading: Boolean = true,
    val pendingCount: Int = 0,
    val transfers: List<CameraUploadsTransferType> = emptyList(),
)

data class CameraUploadsTransferItemUiState(
    val fileTypeResId: Int? = null,
    val previewUri: Uri? = null,
)
