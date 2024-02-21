package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * Compress
 */
class CompressFileForChatUseCase @Inject constructor(
    private val isImageFileUseCase: IsImageFileUseCase,
    private val downscaleImageForChatUseCase: DownscaleImageForChatUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(original: File): File? {
        return when {
            isImageFileUseCase(original.absolutePath) -> {
                downscaleImageForChatUseCase(original)
            }
            // videos will also be compressed in AND-17968

            else -> null
        }
    }
}