package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject


/**
 * Default implementation of [CreateTempFileAndRemoveCoordinates]
 */
class DefaultCreateTempFileAndRemoveCoordinates @Inject constructor(private val fileSystemRepository: FileSystemRepository) :
    CreateTempFileAndRemoveCoordinates {
    override suspend fun invoke(root: String, syncRecord: SyncRecord): String? {
        return fileSystemRepository.createTempFile(root, syncRecord)?.apply {
            fileSystemRepository.removeGPSCoordinates(this)
        }
    }
}
