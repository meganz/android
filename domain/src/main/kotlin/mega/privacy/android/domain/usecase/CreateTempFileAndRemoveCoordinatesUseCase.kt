package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject


/**
 * Create Temp File And RemoveCoordinates Use Case
 */
class CreateTempFileAndRemoveCoordinatesUseCase @Inject constructor(private val fileSystemRepository: FileSystemRepository) {
    /**
     * invoke
     * @param root
     * @param syncRecord
     * @return new created file path
     */
    suspend operator fun invoke(root: String, syncRecord: SyncRecord): String {
        return fileSystemRepository.createTempFile(root, syncRecord).apply {
            fileSystemRepository.removeGPSCoordinates(this)
        }
    }
}
