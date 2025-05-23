package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to remove persistent uri permission
 *
 * @property fileSystemRepository
 */
class RemovePersistentUriPermissionUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Remove persistent uri permission
     *
     * @param uri the uri to remove
     */
    suspend operator fun invoke(uri: UriPath) =
        fileSystemRepository.removePersistentPermission(uri)
}
