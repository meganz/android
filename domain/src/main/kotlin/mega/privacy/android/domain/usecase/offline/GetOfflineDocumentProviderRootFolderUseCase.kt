package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Get Offline Document Provider Root Folder for the User
 */
class GetOfflineDocumentProviderRootFolderUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(): File? {
        val rootFolder = fileSystemRepository.getOfflineFilesRootFolder()
        return if (rootFolder.exists()) rootFolder else null
    }
}
