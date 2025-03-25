package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to get the storage type name: Device Model or SD Card based on file location
 */
class GetFileStorageTypeNameUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     * @param uriPath
     */
    suspend operator fun invoke(uriPath: UriPath) =
        fileSystemRepository.getFileStorageTypeName(
            uriPath.takeIf { it.isPath() }?.value
                ?: fileSystemRepository.getAbsolutePathByContentUri(uriPath.value)
        )
}