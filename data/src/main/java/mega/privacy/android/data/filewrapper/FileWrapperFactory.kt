package mega.privacy.android.data.filewrapper

import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.entity.uri.UriPath

internal class FileWrapperFactory(private val fileGateway: FileGateway) {

    operator fun invoke(uriPath: UriPath): FileWrapper? =
        fileGateway.getDocumentMetadataSync(uriPath.toUri())
            ?.let { (name, isFolder) ->
                FileWrapper(
                    uri = uriPath.value,
                    name = name,
                    isFolder = isFolder,
                    getDetachedFileDescriptorFunction = {
                        fileGateway.getFileDescriptorSync(uriPath, it)?.detachFd()
                    },
                    getChildrenUrisFunction = {
                        if (isFolder) {
                            fileGateway.getFolderChildUrisSync(uriPath.toUri())
                                .map { it.toString() }
                        } else {
                            emptyList()
                        }
                    },
                )
            }
}