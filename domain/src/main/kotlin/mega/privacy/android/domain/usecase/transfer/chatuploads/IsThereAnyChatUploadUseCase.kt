package mega.privacy.android.domain.usecase.transfer.chatuploads

import mega.privacy.android.domain.usecase.transfer.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfer.GetTransferDataUseCase
import mega.privacy.android.domain.usecase.transfer.uploads.IsThereAnyPendingUploadsUseCase
import javax.inject.Inject

/**
 * Use case for checking if is there any chat upload.
 *
 * @property isThereAnyPendingUploadsUseCase [IsThereAnyPendingUploadsUseCase]
 * @property getTransferDataUseCase [GetTransferDataUseCase]
 * @property getTransferByTagUseCase [GetTransferByTagUseCase]
 * @constructor Create empty Is there any chat upload use case
 */
class IsThereAnyChatUploadUseCase @Inject constructor(
    private val isThereAnyPendingUploadsUseCase: IsThereAnyPendingUploadsUseCase,
    private val getTransferDataUseCase: GetTransferDataUseCase,
    private val getTransferByTagUseCase: GetTransferByTagUseCase,
) {

    /**
     * Invoke
     *
     * @return True if there is any chat upload, false otherwise.
     */
    suspend operator fun invoke(): Boolean {
        if (isThereAnyPendingUploadsUseCase()) {
            getTransferDataUseCase()?.let { transferData ->
                transferData.uploadTags.forEach { tag ->
                    getTransferByTagUseCase(tag)?.let { transfer ->
                        if (transfer.isChatUpload() && !transfer.isVoiceClip()) return true
                    }
                }

                return false
            } ?: return false
        } else {
            return false
        }
    }
}