package mega.privacy.android.domain.usecase.transfers.sd

import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.SettingsRepository
import java.io.File
import javax.inject.Inject

/**
 * Moves a file to a destination path in the sd card. User must have set downloads destination on sd for this to work
 */
class MoveFileToSdCardUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val settingsRepository: SettingsRepository,
) {
    /**
     * invoke
     */
    suspend operator fun invoke(file: File, destinationUri: String, subFolders: List<String>) {
        val destination = destinationUri.takeUnless { it.startsWith(File.separator) }
            ?: settingsRepository.getDownloadToSdCardUri() ?: destinationUri
        if (destination.startsWith(File.separator)) {
            throw IllegalArgumentException("Invalid Sd destination for MoveFileToSdCardUseCase. Destination: $destination. OriginalFolder: ${file.parent}. DestinationUri: $destinationUri.")
        }
        fileSystemRepository.moveFileToSd(file, destination, subFolders)
    }
}