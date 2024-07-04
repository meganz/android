package mega.privacy.android.domain.usecase.transfers.offline

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Save uri to device use case
 *
 */
class SaveUriToDeviceUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param name
     * @param source
     * @param destination
     */
    suspend operator fun invoke(
        name: String,
        source: UriPath,
        destination: UriPath,
    ) {
        return when {
            // support below Android 10, user can select an uri from folder picker
            fileSystemRepository.isContentUri(destination.value) -> {
                fileSystemRepository.copyUri(name, source, destination)
            }

            fileSystemRepository.isFileUri(destination.value) -> {
                val destinationFile = fileSystemRepository.getFileFromFileUri(destination.value)
                fileSystemRepository.copyUri(name, source, destinationFile)
            }

            // support Android 10 and above, we created download folder in external storage
            fileSystemRepository.getFileByPath(destination.value) != null -> {
                val destinationFile =
                    fileSystemRepository.getFileByPath(destination.value) ?: return
                fileSystemRepository.copyUri(name, source, destinationFile)
            }

            else -> throw IllegalArgumentException("Invalid destination uri $destination")
        }
    }
}