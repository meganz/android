package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use Case to get the Local DCIM Folder path
 */
class GetLocalDCIMFolderPathUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invocation function
     *
     * Get the Local DCIM Folder path
     */
    operator fun invoke(): String = fileSystemRepository.localDCIMFolderPath
}