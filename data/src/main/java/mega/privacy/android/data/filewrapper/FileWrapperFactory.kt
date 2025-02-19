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
                    childFileExistsFunction = { name ->
                        fileGateway.childFileExistsSync(uriPath, name)
                    },
                    createChildFileFunction = { name, asFolder ->
                        fileGateway.createChildFileSync(uriPath, name, asFolder)?.let {
                            invoke(it)
                        }
                    },
                    getPathFunction = {
                        fileGateway.getExternalPathByContentUriSync(uriPath.value)
                    },
                    getParentUriFunction = {
                        fileGateway.getParentSync(uriPath)?.let {
                            invoke(it)
                        }
                    },
                    deleteFileFunction = {
                        fileGateway.deleteIfItIsAFileSync(uriPath)
                    },
                    deleteFolderIfEmptyFunction = {
                        fileGateway.deleteIfItIsAnEmptyFolder(uriPath)
                    },
                    setModificationTimeFunction = {
                        fileGateway.setLastModifiedSync(uriPath, it * 1_000)
                    },
                    renameFunction = {
                        fileGateway.renameFileSync(uriPath, it)?.let {
                            invoke(it)
                        }
                    }
                )
            }
}