package mega.privacy.android.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileAttributeGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.SDCardGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileSystemRepository
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.URLConnection
import javax.inject.Inject
import javax.inject.Singleton

/**

 * Default implementation of [FileSystemRepository]
 *
 * @property context
 * @property ioDispatcher
 * @property cacheGateway
 * @property fileTypeInfoMapper
 * @property fileGateway
 * @property fileAttributeGateway
 */
@Singleton
internal class FileSystemRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheGateway: CacheGateway,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val fileGateway: FileGateway,
    private val deviceGateway: DeviceGateway,
    private val sdCardGateway: SDCardGateway,
    private val fileAttributeGateway: FileAttributeGateway,
    private val documentFileWrapper: DocumentFileWrapper,
) : FileSystemRepository {

    private val moveSdDocumentMutex = Mutex()

    override val localDCIMFolderPath: String
        get() = fileGateway.localDCIMFolderPath

    override suspend fun getOfflinePath() =
        withContext(ioDispatcher) { fileGateway.getOfflineFilesRootPath() }

    override suspend fun getOfflineBackupsPath() =
        withContext(ioDispatcher) { fileGateway.getOfflineFilesBackupsRootPath() }

    override suspend fun getLocalFile(fileNode: FileNode) = withContext(ioDispatcher) {
        fileGateway.getLocalFile(
            fileName = fileNode.name,
            fileSize = fileNode.size,
            lastModifiedDate = fileNode.modificationTime,
        )
    }

    override suspend fun getFileByPath(path: String): File? = withContext(ioDispatcher) {
        fileGateway.getFileByPath(path)
    }

    override suspend fun createTempFile(
        rootPath: String,
        localPath: String,
        destinationPath: String,
    ) = withContext(ioDispatcher) {
        fileGateway.createTempFile(rootPath, localPath, destinationPath)
        destinationPath
    }

    override suspend fun removeGPSCoordinates(filePath: String) = withContext(ioDispatcher) {
        fileGateway.removeGPSCoordinates(filePath)
    }

    override suspend fun getDiskSpaceBytes(path: String) = withContext(ioDispatcher) {
        deviceGateway.getDiskSpaceBytes(path)
    }

    override suspend fun deleteFile(file: File) = withContext(ioDispatcher) {
        fileGateway.deleteFile(file)
    }

    override suspend fun createDirectory(path: String) =
        withContext(ioDispatcher) {
            fileGateway.createDirectory(path)
        }

    override suspend fun deleteCameraUploadsTemporaryRootDirectory() =
        withContext(ioDispatcher + NonCancellable) {
            val cameraUploadsCacheFolder = cacheGateway.getCameraUploadsCacheFolder()
            fileGateway.deleteDirectory(path = "${cameraUploadsCacheFolder?.absolutePath}${File.separator}")
        }

    override suspend fun createCameraUploadsTemporaryRootDirectory() =
        withContext(ioDispatcher) {
            cacheGateway.getCameraUploadsCacheFolder()
        }

    override suspend fun doesFolderExists(folderPath: String) = withContext(ioDispatcher) {
        fileGateway.isFileAvailable(folderPath)
    }

    override suspend fun isFolderInSDCardAvailable(uriString: String) = withContext(ioDispatcher) {
        val directoryFile = sdCardGateway.getDirectoryFile(uriString)
        fileGateway.isDocumentFileAvailable(directoryFile)
    }

    override suspend fun doesExternalStorageDirectoryExists() = withContext(ioDispatcher) {
        fileGateway.doesExternalStorageDirectoryExists()
    }

    override suspend fun getExternalStorageDirectoryPath(): String = withContext(ioDispatcher) {
        fileGateway.getExternalStorageDirectoryPath()
    }

    override suspend fun doesFileExist(path: String) = withContext(ioDispatcher) {
        File(path).exists()
    }

    override suspend fun getParent(path: String): String = withContext(ioDispatcher) {
        File(path).parent ?: path
    }

    override fun scanMediaFile(paths: Array<String>, mimeTypes: Array<String>) =
        fileGateway.scanMediaFile(paths, mimeTypes)


    override suspend fun getExternalPathByUri(uriString: String): String? =
        withContext(ioDispatcher) {
            fileGateway.getExternalPathByUri(uriString)
        }

    override suspend fun getAbsolutePathByContentUri(uri: String): String? =
        withContext(ioDispatcher) {
            documentFileWrapper.getAbsolutePathFromContentUri(uri.toUri())
        }

    override suspend fun getGuessContentTypeFromName(localPath: String): String? =
        withContext(ioDispatcher) {
            URLConnection.guessContentTypeFromName(localPath)
        }

    override suspend fun getContentTypeFromContentUri(uriPath: UriPath): String? =
        withContext(ioDispatcher) {
            context.contentResolver.getType(uriPath.toUri())
        }

    override suspend fun getVideoGPSCoordinates(uriPath: UriPath): Pair<Double, Double>? =
        withContext(ioDispatcher) {
            if (uriPath.isPath()) {
                fileAttributeGateway.getVideoGPSCoordinates(uriPath.value)
            } else {
                fileAttributeGateway.getVideoGPSCoordinates(uriPath.toUri(), context)
            }
        }

    override suspend fun getPhotoGPSCoordinates(uriPath: UriPath): Pair<Double, Double>? =
        withContext(ioDispatcher) {
            if (uriPath.isPath()) {
                fileAttributeGateway.getPhotoGPSCoordinates(uriPath.value)
            } else {
                fileGateway.getInputStream(uriPath)?.let { inputStream ->
                    fileAttributeGateway.getPhotoGPSCoordinates(inputStream)
                }
            }
        }

    override suspend fun setLastModified(path: String, timestamp: Long) =
        withContext(ioDispatcher) {
            fileGateway.setLastModified(path, timestamp)
        }

    override suspend fun saveTextOnContentUri(uri: String, text: String) =
        withContext(ioDispatcher) {
            fileGateway.saveTextOnContentUri(uri, text)
        }

    override suspend fun getUriForFile(file: File, authority: String): String =
        withContext(ioDispatcher) {
            runCatching { fileGateway.getUriForFile(file, authority).toString() }
                .onFailure {
                    if (it !is IllegalArgumentException) {
                        throw it
                    }
                }.getOrNull() ?: Uri.fromFile(file).toString()
        }

    override suspend fun deleteFolderAndItsFiles(path: String) {
        val file = File(path)
        fileGateway.deleteFolderAndSubFolders(file)
    }

    override suspend fun getOfflineFolder(): File =
        withContext(ioDispatcher) { fileGateway.getOfflineFolder() }

    override suspend fun getTotalSize(file: File?): Long =
        withContext(ioDispatcher) { fileGateway.getTotalSize(file) }

    override suspend fun checkFileExistsByUriPath(uriPath: String?): String? =
        withContext(ioDispatcher) {
            try {
                if (uriPath != null && File(URI.create(uriPath).path).exists()) {
                    uriPath
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }

    override suspend fun isSDCardPathOrUri(localPath: String) = withContext(ioDispatcher) {
        sdCardGateway.doesFolderExists(localPath) || sdCardGateway.isSDCardUri(localPath)
    }

    override suspend fun isSDCardCachePath(localPath: String) = withContext(ioDispatcher) {
        sdCardGateway.isSDCardCachePath(localPath)
    }

    override suspend fun copyFilesToDocumentUri(
        source: File,
        destinationUri: UriPath,
    ) = withContext(ioDispatcher) {
        val uri = destinationUri.value.toUri()
        val destination = documentFileWrapper.fromUri(uri)
            ?: throw FileNotFoundException("Content uri doesn't exist: $destinationUri")

        fileGateway.copyFilesToDocumentFolder(source, destination)
    }

    override suspend fun copyFiles(source: File, destination: File) = withContext(ioDispatcher) {
        fileGateway.copyFileToFolder(source, destination)
    }

    override fun getFileTypeInfoByName(name: String, duration: Int): FileTypeInfo =
        fileTypeInfoMapper(name, duration)

    override suspend fun moveFileToSd(
        file: File,
        destinationUri: String,
        subFolders: List<String>,
    ) = withContext(ioDispatcher) {
        val sourceDocument = documentFileWrapper.fromFile(file)
        val sdCardUri = destinationUri.toUri()

        val destDocument = moveSdDocumentMutex.withLock {
            documentFileWrapper.getSdDocumentFile(
                sdCardUri,
                subFolders,
                file.name,
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                    ?: "application/octet-stream"
            )
        }

        try {
            if (destDocument != null) {
                val inputStream =
                    context.contentResolver.openInputStream(sourceDocument.uri)
                val outputStream =
                    context.contentResolver.openOutputStream(destDocument.uri)

                inputStream?.use { input ->
                    outputStream?.use { output ->
                        input.copyTo(output)
                    }
                }
                file.delete()
                return@withContext true
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext false
    }

    override suspend fun moveDirectoryToSd(
        directory: File,
        destinationUri: String,
    ): Boolean = withContext(ioDispatcher) {
        val directorySdCardStringUri = destinationUri.toUri().let { sdCardUri ->
            moveSdDocumentMutex.withLock {
                documentFileWrapper.fromUri(sdCardUri)?.let {
                    it.findFile(directory.name) ?: it.createDirectory(directory.name)
                }?.uri?.toString()
            }
        } ?: run {
            Timber.w("Error getting SD card uri")
            null
        }

        directory.listFiles()?.forEach { childFile ->
            if (childFile.isDirectory) {
                directorySdCardStringUri?.let { moveDirectoryToSd(childFile, it) }
                    ?: Timber.w("Error moving directory to SD card")
            } else {
                moveFileToSd(childFile, destinationUri, listOf(directory.name))
            }
        }

        return@withContext directory.delete()
    }

    override suspend fun createNewImageUri(fileName: String): String? = withContext(ioDispatcher) {
        fileGateway.createNewImageUri(fileName)?.toString()
    }

    override suspend fun createNewVideoUri(fileName: String): String? = withContext(ioDispatcher) {
        fileGateway.createNewVideoUri(fileName)?.toString()
    }

    override suspend fun isFileUri(uriString: String) = withContext(ioDispatcher) {
        fileGateway.isFileUri(uriString)
    }

    override suspend fun isFilePath(path: String) = withContext(ioDispatcher) {
        fileGateway.isFilePath(path)
    }

    override suspend fun isFolderPath(path: String) = withContext(ioDispatcher) {
        fileGateway.isFolderPath(path)
    }

    override suspend fun isFolderContentUri(uri: String) = withContext(ioDispatcher) {
        fileGateway.isFolderContentUri(uri)
    }

    override suspend fun getFileFromFileUri(uriString: String) = withContext(ioDispatcher) {
        fileGateway.getFileFromUriFile(uriString)
    }

    override suspend fun isContentUri(uriString: String): Boolean = withContext(ioDispatcher) {
        fileGateway.isContentUri(uriString)
    }

    override suspend fun isDocumentUri(uri: UriPath): Boolean = withContext(ioDispatcher) {
        fileGateway.isDocumentUri(uri)
    }

    override suspend fun isExternalStorageContentUri(uriString: String): Boolean =
        withContext(ioDispatcher) {
            fileGateway.isExternalStorageContentUri(uriString)
        }

    override suspend fun getFileNameFromUri(uriString: String): String? =
        withContext(ioDispatcher) {
            fileGateway.getFileNameFromUri(uriString)
        }

    override suspend fun getFileSizeFromUri(uriString: String): Long? =
        withContext(ioDispatcher) {
            fileGateway.getFileSizeFromUri(uriString)
        }

    override suspend fun copyContentUriToFile(sourceUri: UriPath, targetFile: File) {
        withContext(ioDispatcher) {
            fileGateway.copyContentUriToFile(sourceUri, targetFile)
        }
    }

    override suspend fun getFileSiblingByUri(uriString: String) =
        withContext(ioDispatcher) {
            val currentFile = uriString.toUri().toFile()
            currentFile.parentFile?.listFiles()?.toList()?.filter {
                it.isFile && it.exists() && it.canRead()
            } ?: listOf(currentFile)
        }

    override suspend fun downscaleImage(original: UriPath, destination: File, maxPixels: Long) {
        fileGateway.downscaleImage(original, destination, maxPixels)
    }

    override suspend fun deleteVoiceClip(name: String): Boolean =
        withContext(ioDispatcher + NonCancellable) {
            cacheGateway.getVoiceClipFile(name)?.let { fileGateway.deleteFile(it) } ?: false
        }

    override suspend fun getFileTypeInfo(file: File) =
        withContext(ioDispatcher) {
            val duration = fileAttributeGateway.getVideoDuration(file.absolutePath)
            fileTypeInfoMapper(file.name, duration?.inWholeSeconds?.toInt() ?: 0)
        }

    override suspend fun getFileTypeInfo(uriPath: UriPath, fileName: String) =
        withContext(ioDispatcher) {
            val duration = fileAttributeGateway.getVideoDuration(uriPath.value)
            fileTypeInfoMapper(fileName, duration?.inWholeSeconds?.toInt() ?: 0)
        }

    override suspend fun deleteFileByUri(uri: String): Boolean = withContext(ioDispatcher) {
        fileGateway.deleteFileByUri(uri.toUri())
    }

    override suspend fun getFilesInDocumentFolder(uri: UriPath): DocumentFolder =
        withContext(ioDispatcher) {
            fileGateway.getFilesInDocumentFolder(uri)
        }

    override fun searchFilesInDocumentFolderRecursive(
        folder: UriPath,
        query: String,
    ): Flow<DocumentFolder> = fileGateway.searchFilesInDocumentFolderRecursive(folder, query)
        .flowOn(ioDispatcher)

    override suspend fun copyUri(name: String, source: UriPath, destination: File) =
        withContext(ioDispatcher) {
            fileGateway.copyUriToDocumentFolder(
                name = name,
                source = source.value.toUri(),
                destination = documentFileWrapper.fromFile(destination)
            )
        }

    override suspend fun copyUri(name: String, source: UriPath, destination: UriPath) =
        withContext(ioDispatcher) {
            val uri = destination.value.toUri()
            val destinationUri = documentFileWrapper.fromUri(uri)
                ?: throw FileNotFoundException("Content uri doesn't exist: $destination")

            fileGateway.copyUriToDocumentFolder(
                name = name,
                source = source.value.toUri(),
                destination = destinationUri
            )
        }

    override fun isPathInsecure(path: String): Boolean =
        fileGateway.isPathInsecure(path)

    override fun isMalformedPathFromExternalApp(action: String?, path: String): Boolean =
        fileGateway.isMalformedPathFromExternalApp(action, path)

    override suspend fun getDocumentFileName(uri: UriPath): String = withContext(ioDispatcher) {
        val rawUri = uri.value.toUri()
        val document = documentFileWrapper.fromUri(rawUri)
            ?: throw FileNotFoundException("Content uri doesn't exist: $rawUri")

        document.name.orEmpty()
    }

    override suspend fun getDocumentEntities(uris: List<UriPath>): List<DocumentEntity> =
        withContext(ioDispatcher) {
            fileGateway.getDocumentEntities(uris.map { it.value.toUri() })
        }

    override suspend fun getDocumentEntity(uri: UriPath): DocumentEntity? =
        getDocumentEntities(listOf(uri)).firstOrNull()

    override suspend fun getFileFromUri(uri: UriPath): File? = withContext(ioDispatcher) {
        fileGateway.getFileFromUri(uri.value.toUri())?.also {
            Timber.d("getFileFromUri uri: $uri, file path: $it")
        }
    }

    override suspend fun renameFileAndDeleteOriginal(
        originalUriPath: UriPath,
        newFilename: String,
    ): File = withContext(ioDispatcher) {
        // Create new File with the new Filename
        val oldFile = File(originalUriPath.value)
        val newFile = File(oldFile.parentFile, newFilename)

        // Rename File and delete the old File
        if (fileGateway.renameFile(oldFile, newFilename)) fileGateway.deleteFile(oldFile)

        newFile
    }

    override suspend fun getFileLengthFromSdCardContentUri(
        fileContentUri: String,
    ) = fileContentUri.toUri()?.let { uri ->
        documentFileWrapper.fromSingleUri(uri)?.length() ?: 0L
    } ?: 0L

    override suspend fun deleteFileFromSdCardContentUri(fileContentUri: String) =
        fileContentUri.toUri()?.let { uri ->
            documentFileWrapper.fromSingleUri(uri)?.delete() ?: false
        } ?: false

    override suspend fun canReadUri(stringUri: String) =
        fileGateway.canReadUri(stringUri)

    override suspend fun getOfflineFilesRootFolder(): File =
        File(fileGateway.getOfflineFilesRootPath())

    override suspend fun getFileStorageTypeName(path: String?) = withContext(ioDispatcher) {
        fileGateway.getFileStorageTypeName(path)
    }

    override suspend fun doesUriPathExist(uriPath: UriPath) = withContext(ioDispatcher) {
        fileGateway.doesUriPathExist(uriPath)
    }

    override suspend fun removePersistentPermission(uriPath: UriPath) {
        fileGateway.removePersistentPermission(uriPath)
    }

    override suspend fun hasPersistedPermission(uriPath: UriPath, writePermission: Boolean) =
        withContext(ioDispatcher) {
            fileGateway.hasPersistedPermission(uriPath.toUri(), writePermission)
        }

    override suspend fun takePersistablePermission(uriPath: UriPath, writePermission: Boolean) {
        withContext(ioDispatcher) {
            fileGateway.takePersistablePermission(uriPath.toUri(), writePermission)
        }
    }
}
