package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.repository.FileRepository
import javax.inject.Inject


/**
 * Default implementation of [CreateTempFileAndRemoveCoordinates]
 */
class DefaultCreateTempFileAndRemoveCoordinates @Inject constructor(private val fileRepository: FileRepository) :
    CreateTempFileAndRemoveCoordinates {
    override suspend fun invoke(root: String, syncRecord: SyncRecord): String? {
        return fileRepository.createTempFile(root, syncRecord)?.apply {
            fileRepository.removeGPSCoordinates(this)
        }
    }
}
