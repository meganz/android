package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

class GetExternalStorageDirectoryPathUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Get the external storage directory path
     *
     * @return the external storage directory path
     */
    suspend operator fun invoke(): String? {
        return fileSystemRepository.getExternalStorageDirectoryPath()
    }
}
