package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import javax.inject.Inject

class GetUploadFileSizeDifferenceUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val getFileByPathUseCase: GetFileByPathUseCase,
) {

    /**
     * Get the difference in file size between the original file and the temporary file.
     *
     * @param record [CameraUploadsRecord]
     * @return The difference in file size, or null if either file does not exist.
     */
    suspend operator fun invoke(record: CameraUploadsRecord): Long? {
        val tempFilePath =
            record.tempFilePath.takeIf { fileSystemRepository.doesFileExist(it) }
        return tempFilePath?.let { path ->
            val tempFileSize = getFileByPathUseCase(path)?.length()
            val originalFileSize = record.fileSize
            if (tempFileSize != null && originalFileSize != tempFileSize) {
                originalFileSize - tempFileSize
            } else {
                null
            }
        }
    }
}