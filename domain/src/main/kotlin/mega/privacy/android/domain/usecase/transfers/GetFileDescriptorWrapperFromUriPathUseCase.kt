package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.entity.file.FileDescriptorWrapper
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to get a [FileDescriptorWrapper] from an [UriPath]
 */
class GetFileDescriptorWrapperFromUriPathUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(uriPath: UriPath): FileDescriptorWrapper? =
        fileSystemRepository.getDocumentMetadata(uriPath)
            ?.let { (name, isFolder) ->
                FileDescriptorWrapper(
                    uriPath = uriPath,
                    name = name,
                    isFolder = isFolder,
                    getDetachedFileDescriptorFunction = {
                        fileSystemRepository.getDetachedFileDescriptor(uriPath, it)
                    },
                    getChildrenUrisFunction = {
                        if (isFolder) {
                            fileSystemRepository.getFolderChildUriPaths(uriPath)
                        } else {
                            emptyList()
                        }
                    },
                )
            }
}