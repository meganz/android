package mega.privacy.android.data.model

import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.exception.MegaException
import java.io.File

/**
 * Data class to model Result of Full Image download from Server
 *
 * @param imageResult ImageResult
 * @param deleteFile File
 * @param exception MegaException
 */
data class FullImageDownloadResult(
    val imageResult: ImageResult? = null,
    val deleteFile: File? = null,
    val exception: MegaException? = null,
)

