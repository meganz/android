package mega.privacy.android.domain.entity.photos

sealed interface DownloadPhotoResult {
    data class Success(
        val previewFilePath: String?,
        val thumbnailFilePath: String?,
    ) : DownloadPhotoResult

    data object EmptyFilePath : DownloadPhotoResult

    data object Error : DownloadPhotoResult

    data object Idle : DownloadPhotoResult
}
