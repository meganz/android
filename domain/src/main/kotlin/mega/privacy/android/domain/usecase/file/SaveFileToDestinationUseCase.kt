package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Use case to save a file to a destination UriPath.
 *
 * This use case copies a file from the source [File] to the destination [UriPath].
 * The destination must be a valid folder document URI that can be accessed through the Android DocumentProvider API.
 *
 */
class SaveFileToDestinationUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke the use case to save a file to the destination.
     *
     * @param source The source file or folder to copy
     * @param destination The destination URI path where the file will be saved, it should be a folder
     */
    suspend operator fun invoke(
        source: File,
        destination: UriPath,
    ) {
        fileSystemRepository.copyFilesToDocumentUri(source, destination)
    }
}