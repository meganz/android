package mega.privacy.android.domain.usecase.transfers.sd

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Moves a file to a destination path in the sd card. User must have set downloads destination on sd for this to work
 */
class MoveFileToSdCardUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * invoke
     */
    suspend operator fun invoke(file: File, destinationUri: String, subFolders: List<String>) {
        fileSystemRepository.moveFileToSd(file, destinationUri, subFolders)
    }
}