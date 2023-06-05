package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Does path have sufficient space
 */
class DoesPathHaveSufficientSpaceUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param path
     * @param requiredSpace
     * @return true if path has sufficient space, otherwise false
     */
    suspend operator fun invoke(path: String, requiredSpace: Long): Boolean = runCatching {
        fileSystemRepository.getDiskSpaceBytes(path) > requiredSpace
    }.getOrDefault(false)
}