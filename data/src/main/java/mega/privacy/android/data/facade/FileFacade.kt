package mega.privacy.android.data.facade

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap.CompressFormat
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns.DATA
import android.provider.MediaStore.MediaColumns.DATE_ADDED
import android.provider.MediaStore.MediaColumns.DATE_MODIFIED
import android.provider.MediaStore.MediaColumns.DATE_TAKEN
import android.provider.MediaStore.MediaColumns.DISPLAY_NAME
import android.provider.MediaStore.MediaColumns.SIZE
import android.provider.MediaStore.VOLUME_EXTERNAL
import android.provider.MediaStore.VOLUME_INTERNAL
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.mapper.file.DocumentFileMapper
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.document.DocumentMetadata
import mega.privacy.android.domain.entity.file.FileStorageType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.FileNotCreatedException
import mega.privacy.android.domain.exception.NotEnoughStorageException
import nz.mega.sdk.AndroidGfxProcessor
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.Stack
import javax.inject.Inject
import kotlin.Result.Companion.failure
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Intent extra data for node handle
 */
const val INTENT_EXTRA_NODE_HANDLE = "NODE_HANDLE"

/**
 * File facade
 *
 * Refer to [mega.privacy.android.app.utils.FileUtil]
 */
internal class FileFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentFileMapper: DocumentFileMapper,
    private val deviceGateway: DeviceGateway,
    private val documentFileWrapper: DocumentFileWrapper,
) : FileGateway {

    override val localDCIMFolderPath: String
        get() = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM
        )?.absolutePath ?: ""

    override suspend fun getTotalSize(file: File?): Long {
        file ?: return -1L
        return file.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    override suspend fun getTotalSizeRecursive(folderPath: UriPath): Long {
        return try {
            val documentFolder = getFilesInDocumentFolder(folderPath)
            calculateRecursiveSize(documentFolder.files)
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate recursive size for $folderPath")
            0L
        }
    }

    private suspend fun calculateRecursiveSize(files: List<DocumentEntity>): Long {
        var totalSize = 0L
        for (file in files) {
            if (file.isFolder) {
                // Skip tmp folders as they contain temporary files that shouldn't be counted as debris
                if (file.name.lowercase() != TMP_FOLDER_NAME) {
                    // Recursively calculate size for subdirectories
                    totalSize += getTotalSizeRecursive(file.uri)
                }
            } else {
                // Add file size
                totalSize += file.size
            }
        }
        return totalSize
    }

    override fun deleteFolderAndSubFolders(folder: File?): Boolean {
        folder ?: return false
        Timber.d("deleteFolderAndSubFolders: ${folder.absolutePath}")
        return folder.deleteRecursively().also {
            Timber.d("Folder delete success $it for ${folder.absolutePath}")
        }
    }

    @Deprecated("File already exposes exists() function", ReplaceWith("file?.exists() == true"))
    override suspend fun isFileAvailable(file: File?): Boolean = file?.exists() == true
    override suspend fun isFileAvailable(fileString: String): Boolean = File(fileString).exists()
    override suspend fun isDocumentFileAvailable(documentFile: DocumentFile?) =
        documentFile?.exists() == true

    override suspend fun doesExternalStorageDirectoryExists(): Boolean =
        Environment.getExternalStorageDirectory() != null

    override suspend fun getExternalStorageDirectoryPath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    override suspend fun buildDefaultDownloadDir(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?.let { downloadsDir ->
                File(downloadsDir, DOWNLOAD_DIR)
            } ?: context.filesDir

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun getLocalFile(
        fileName: String,
        fileSize: Long,
        lastModifiedDate: Long,
    ) = runCatching {
        val selectionArgs = arrayOf(
            fileName,
            fileSize.toString(),
            lastModifiedDate.toString()
        )
        getExternalFile(selectionArgs) ?: getInternalFile(selectionArgs)
    }.getOrNull()

    override suspend fun getFileByPath(path: String): File? {
        val file = File(path)
        return if (file.exists()) {
            file
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getExternalFile(
        selectionArgs: Array<String>,
    ) = getExternalCursor(selectionArgs)?.let { cursor ->
        getFileFromCursor(cursor).also { cursor.close() }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getInternalFile(
        selectionArgs: Array<String>,
    ) = getInternalCursor(selectionArgs)?.let { cursor ->
        getFileFromCursor(cursor).also { cursor.close() }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getExternalCursor(
        selectionArgs: Array<String>,
    ) = getNonEmptyCursor(
        MediaStore.Files.getContentUri(VOLUME_EXTERNAL),
        selectionArgs,
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getInternalCursor(
        selectionArgs: Array<String>,
    ) = getNonEmptyCursor(
        MediaStore.Files.getContentUri(VOLUME_INTERNAL),
        selectionArgs,
    )

    private fun getNonEmptyCursor(
        uri: Uri,
        selectionArgs: Array<String>,
    ): Cursor? {
        val cursor = context.contentResolver.query(
            /* uri = */
            uri,
            /* projection = */
            arrayOf(DATA),
            /* selection = */
            "$DISPLAY_NAME = ? AND $SIZE = ? AND $DATE_MODIFIED = ?",
            /* selectionArgs = */
            selectionArgs,
            /* sortOrder = */
            null,
        ) ?: return null

        return if (cursor.moveToFirst()) {
            cursor
        } else {
            cursor.close()
            null
        }
    }

    private fun getFileFromCursor(cursor: Cursor): File? {
        val path = cursor.getString(cursor.getColumnIndexOrThrow(DATA))
        return File(path).takeIf { it.exists() }
    }

    override suspend fun getOfflineFilesRootPath() =
        context.filesDir.absolutePath + File.separator + OFFLINE_DIR

    override suspend fun getOfflineFilesBackupsRootPath() =
        context.filesDir.absolutePath + File.separator + OFFLINE_DIR + File.separator + "in"

    override suspend fun removeGPSCoordinates(filePath: String) {
        try {
            val exif = ExifInterface(filePath)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, LAT_LNG)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, REF_LAT_LNG)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, LAT_LNG)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, REF_LAT_LNG)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, LAT_LNG)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, REF_LAT_LNG)
            exif.saveAttributes()
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    override suspend fun copyFile(source: File, destination: File) {
        if (source.absolutePath != destination.absolutePath) {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    override suspend fun copyFileToFolder(source: File, destination: File): Int {
        return copyFilesToDocumentFolder(source, DocumentFile.fromFile(destination))
    }

    override suspend fun createTempFile(rootPath: String, localPath: String, newPath: String) {
        val srcFile = File(localPath)
        if (!srcFile.exists()) {
            Timber.e("Source File doesn't exist")
            throw FileNotFoundException()
        }
        val hasEnoughSpace = hasEnoughStorage(rootPath, srcFile)
        if (!hasEnoughSpace) {
            Timber.e("Not Enough Storage")
            throw NotEnoughStorageException()
        }
        val destinationFile = File(newPath)
        try {
            copyFile(srcFile, destinationFile)
        } catch (e: IOException) {
            Timber.e(e)
            throw FileNotCreatedException()
        }
    }

    override suspend fun hasEnoughStorage(rootPath: String, file: File) =
        runCatching { hasEnoughStorage(rootPath, file.length()) }.onFailure { Timber.e(it) }
            .getOrDefault(false)

    override suspend fun hasEnoughStorage(rootPath: String, length: Long) =
        runCatching { StatFs(rootPath).availableBytes >= length }.onFailure { Timber.e(it) }
            .getOrDefault(false)

    override suspend fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    override suspend fun createDirectory(path: String) = run {
        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        directory
    }

    override suspend fun deleteDirectory(path: String) = run {
        val directory = File(path)
        directory.deleteRecursively()
    }

    override fun scanMediaFile(paths: Array<String>, mimeTypes: Array<String>) {
        MediaScannerConnection.scanFile(
            context,
            paths,
            mimeTypes,
            null
        )
    }

    override suspend fun getExternalPathByUri(uriString: String) =
        getExternalPathByUriSync(uriString)

    override fun getExternalPathByUriSync(uriString: String): String? = with(uriString) {
        if (startsWith(File.separator)) {
            uriString
        } else {
            toUri().let { uri ->
                documentFileWrapper.getAbsolutePathFromContentUri(uri)
            }
        }
    }

    override suspend fun deleteFilesInDirectory(directory: File) {
        directory.listFiles()?.forEach {
            it.delete()
        }
    }

    override suspend fun buildExternalStorageFile(filePath: String): File =
        File(Environment.getExternalStorageDirectory().absolutePath + filePath)

    override suspend fun renameFile(oldFile: File, newName: String): Boolean {
        if (oldFile.exists()) {
            val newRKFile = File(oldFile.parentFile, newName)
            return oldFile.renameTo(newRKFile)
        }
        return false
    }

    override suspend fun getAbsolutePath(path: String) =
        File(path).takeIf { it.exists() }?.absolutePath

    override suspend fun setLastModified(path: String, timestamp: Long) =
        File(path).takeIf { it.exists() }?.setLastModified(timestamp)

    override suspend fun saveTextOnContentUri(uri: String, text: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = context.contentResolver.openFileDescriptor(uri.toUri(), "w")
                file?.use {
                    FileOutputStream(file.fileDescriptor).use {
                        it.write(text.toByteArray())
                    }
                } ?: return@runCatching false
            }.fold(
                onSuccess = { true },
                onFailure = { false }
            )
        }

    override suspend fun getUriForFile(file: File, authority: String): Uri =
        withContext(Dispatchers.IO) {
            FileProvider.getUriForFile(context, authority, file)
        }

    override suspend fun getOfflineFolder() =
        createDirectory(context.filesDir.toString() + File.separator + OFFLINE_DIR)

    override suspend fun createNewImageUri(fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // use default location for below Android Q
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DCIM}/$PHOTO_DIR"
                )
            }
        }

        val contentResolver = context.contentResolver
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    override suspend fun createNewVideoUri(fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DCIM}/$PHOTO_DIR"
                )
            }
        }

        val contentResolver = context.contentResolver
        return contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    override suspend fun isFileUri(uriString: String) = isFileUri(uriString.toUri())

    override suspend fun isFolderContentUri(uriString: String) =
        getDocumentFileFromUri(uriString.toUri())?.isDirectory == true

    private fun isFileUri(uri: Uri) = uri.scheme == "file"

    override suspend fun isFilePath(path: String) = File(path).isFile

    override suspend fun isFolderPath(path: String) = File(path).isDirectory

    override suspend fun getFileFromUriFile(uriString: String): File =
        uriString.toUri().toFile()

    override suspend fun isContentUri(uriString: String) = uriString.toUri().scheme == "content"

    override suspend fun isDocumentUri(uri: UriPath): Boolean =
        DocumentsContract.isDocumentUri(context, uri.value.toUri())

    override suspend fun isExternalStorageContentUri(uriString: String) =
        with(uriString.toUri()) {
            scheme == "content" && authority?.startsWith("com.android.externalstorage") == true
        }

    override suspend fun getFileNameFromUri(uriString: String): String? = with(uriString.toUri()) {
        getDocumentFileFromUri(this)?.let { documentFile ->
            documentFile.name
        } ?: getFileNameFromContentResolver(this)
    }

    override suspend fun getFileSizeFromUri(uriString: String): Long? =
        runCatching {
            getFileSizeFromContentResolver(uriString.toUri())
        }.onFailure { Timber.e(it, "Error getting file size from Uri $uriString") }
            .getOrNull()

    override suspend fun copyContentUriToFile(sourceUri: UriPath, targetFile: File) {
        val uri = sourceUri.value.toUri()
        require(uri.scheme == "content")
        val isTreeUri = DocumentsContract.isTreeUri(uri)
        val document = if (isTreeUri) {
            DocumentFile.fromTreeUri(context, uri)
        } else {
            DocumentFile.fromSingleUri(context, uri)
        } ?: run {
            Timber.e("Content uri doesn't exist: $sourceUri")
            return
        }
        if (document.isDirectory) {
            copyDirectory(document, targetFile)
        } else {
            copyFile(document, targetFile)
        }
    }

    private suspend fun copyDirectory(sourceDir: DocumentFile, targetDir: File) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        sourceDir.listFiles().forEach { file ->
            val newFile = File(targetDir, file.name ?: return@forEach)
            if (file.isDirectory) {
                copyDirectory(file, newFile)
            } else {
                copyFile(file, newFile)
            }
        }
    }

    private fun copyFile(sourceFile: DocumentFile, targetFile: File) {
        context.contentResolver.openInputStream(sourceFile.uri)?.use { inputStream ->
            targetFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            (sourceFile.lastModified().takeIf { it > 0 }
                ?: getLastModifiedFromContentResolver(sourceFile.uri)).let { lastModified ->
                lastModified.takeIf { it > 0 }?.let {
                    targetFile.setLastModified(lastModified)
                }
            }
        }
    }

    private fun getLastModifiedFromContentResolver(uri: Uri) = runCatching {
        context.contentResolver.acquireContentProviderClient(uri)
            ?.use { client ->
                client.query(uri, null, null, null, null)
                    ?.use { cursor ->
                        var lastModified = 0L

                        cursor.moveToFirst()
                        cursor.getColumnIndex(DATE_MODIFIED).takeIf { it != -1 }?.let { index ->
                            /* As MediaStore documentation states, the date is in seconds.
                            As we work with this property using milliseconds, we need to convert it. */
                            lastModified = cursor.getLong(index) * 1000
                        } ?: cursor.getColumnIndex(DATE_ADDED).takeIf { it != -1 }?.let { index ->
                            /* As MediaStore documentation states, the date is in seconds.
                            As we work with this property using milliseconds, we need to convert it. */
                            lastModified = cursor.getLong(index) * 1000
                        } ?: cursor.getColumnIndex(DATE_TAKEN).takeIf { it != -1 }?.let { index ->
                            lastModified = cursor.getLong(index)
                        }

                        if (lastModified > getCurrentTimeWithToleranceMultiplier(10)) {
                            /* Some OS does not follow MediaStore documentation and implements the values using
                            their own units. This ensures we set the date to the correct value in case the OS
                            does not follow it. */
                            lastModified /= 1000
                        }

                        Timber.d("Last modified: %s", lastModified);
                        lastModified
                    } ?: 0
            } ?: 0
    }.onFailure { Timber.e("Error getting last modified from Uri: $uri") }.getOrElse { 0L }

    private fun getCurrentTimeWithToleranceMultiplier(toleranceMultiplier: Int) =
        System.currentTimeMillis() * toleranceMultiplier

    override suspend fun downscaleImage(original: UriPath, destination: File, maxPixels: Long) {
        val orientation = AndroidGfxProcessor.getExifOrientation(original.value)
        val fileRect = AndroidGfxProcessor.getImageDimensions(original.value, orientation)
        val fileBitmap = AndroidGfxProcessor.getBitmap(
            original.value,
            fileRect,
            orientation,
            fileRect.right,
            fileRect.bottom
        )
        if (fileBitmap == null) {
            Timber.e("Bitmap NULL when decoding image file for upload it to chat.")
            return
        }

        var width = fileBitmap.width
        var height = fileBitmap.height
        val totalPixels = width * height
        if (totalPixels == 0) {
            Timber.e("Bitmap is not valid, it has 0 pixels")
            return
        }
        val division: Float = maxPixels.toFloat() / totalPixels.toFloat()
        val factor = sqrt(division.toDouble()).coerceAtMost(1.0).toFloat()
        if (factor < 1) {
            width = (width * factor).toInt()
            height = (height * factor).toInt()
            Timber.d(
                "DATA connection factor<1\n" +
                        "totalPixels: $totalPixels\n" +
                        "width: $width\n" +
                        "height: $height\n" +
                        "DOWNSCALE_IMAGES_PX/totalPixels: $division\n" +
                        "Math.sqrt(DOWNSCALE_IMAGES_PX/totalPixels): ${sqrt(division)}"
            )
            val scaleBitmap =
                fileBitmap.scale(width, height)

            val fOut: FileOutputStream
            try {
                fOut = FileOutputStream(destination)
                scaleBitmap.compress(original.getCompressFormat(), 100, fOut)
                fOut.flush()
                fOut.close()
            } catch (e: java.lang.Exception) {
                Timber.e(e, "Exception compressing image file for upload it to chat.")
            } finally {
                scaleBitmap.recycle()
            }

        } else {
            Timber.d("No need to scale the image as it is smaller than the target")
        }
        fileBitmap.recycle()
    }

    override suspend fun deleteFileByUri(uri: Uri): Boolean =
        context.contentResolver.delete(uri, null, null) > 0

    override suspend fun getFilesInDocumentFolder(folder: UriPath): DocumentFolder {
        val uri = folder.toUri()
        val semaphore = Semaphore(10)
        val document = if (isFileUri(uri)) {
            DocumentFile.fromFile(uri.toFile())
        } else {
            DocumentFile.fromTreeUri(context, uri)
        } ?: throw FileNotFoundException()
        val files = document.listFiles()
        val folders = files.filter { it.isDirectory }
        val countMap = coroutineScope {
            folders.map {
                async {
                    semaphore.withPermit {
                        val childFiles = it.listFiles()
                        val totalDirectory = childFiles.count { it.isDirectory }
                        val totalFiles = childFiles.size - totalDirectory
                        totalFiles to totalDirectory
                    }
                }
            }
        }.awaitAll()
            .mapIndexed { index, pair ->
                val file = folders[index]
                file.uri to pair
            }.toMap()

        // length, lastModified is heavy operation, so we do it in parallel
        val entities = coroutineScope {
            files.mapNotNull { file ->
                async {
                    semaphore.withPermit {
                        val documentUri = file.uri
                        documentFileMapper(
                            file = file,
                            numFiles = countMap[documentUri]?.first ?: 0,
                            numFolders = countMap[documentUri]?.second ?: 0
                        )
                    }
                }
            }.awaitAll()
        }
        return DocumentFolder(entities)
    }

    override fun searchFilesInDocumentFolderRecursive(
        folder: UriPath,
        query: String,
    ): Flow<DocumentFolder> = flow {
        // using stack to avoid recursive call and optimize memory usage
        val stack = Stack<DocumentFile>()
        val uri = folder.value.toUri()
        val document = if (isFileUri(folder.value)) {
            DocumentFile.fromFile(uri.toFile())
        } else {
            DocumentFile.fromTreeUri(context, uri)
        } ?: throw FileNotFoundException()
        stack.addAll(document.listFiles())
        val result = mutableListOf<DocumentEntity>()
        while (stack.isNotEmpty() && coroutineContext.isActive) {
            val file = stack.pop()
            val childFiles = if (file.isDirectory) file.listFiles().toList() else emptyList()
            if (file.name.orEmpty().contains(other = query, ignoreCase = true)) {
                result.add(
                    documentFileMapper(
                        file = file,
                        numFiles = childFiles.count { it.isFile },
                        numFolders = childFiles.count { it.isDirectory },
                    )
                )
                emit(DocumentFolder(result))
            }
            if (file.isDirectory) {
                stack.addAll(childFiles)
            }
        }
        emit(DocumentFolder(result))
    }

    @Suppress("Deprecation")
    private suspend fun UriPath.getCompressFormat(): CompressFormat = when (
        (if (this.isPath()) this.value else getFileNameFromUri(this.value))?.substringAfterLast(".")) {
        "jpeg", "jpg" -> CompressFormat.JPEG
        "png" -> CompressFormat.PNG
        // CompressFormat.WEBP is deprecated in API Level 30. Update function to handle both
        // CompressFormat.WEB_LOSSY and CompressFormat.WEB_LOSSLESS
        "webp" -> CompressFormat.WEBP
        else -> CompressFormat.JPEG
    }

    override suspend fun copyFilesToDocumentFolder(
        source: File,
        destination: DocumentFile,
    ): Int {
        if (!destination.isDirectory) throw IllegalArgumentException("Destination is not a directory")
        var totalFile = 0
        if (source.isDirectory) {
            val files = source.listFiles()
            val newFolder = destination.createDirectory(source.name) ?: return 0
            files?.forEach {
                totalFile += copyFilesToDocumentFolder(it, newFolder)
            }
        } else {
            val fileName = getFileNameIfHasNameCollision(destination, source.name)
            val fileNameWithoutExtension = fileName.substringBeforeLast(".")
            val mimeType = getMimeTypeFromExtension(source.extension)
            val newFile = destination.createFile(mimeType, fileNameWithoutExtension)
            newFile?.uri?.let { newUri ->
                context.contentResolver.openOutputStream(newUri)?.use { output ->
                    source.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
            totalFile = 1
        }
        return totalFile
    }

    override suspend fun copyUriToDocumentFolder(
        name: String,
        source: Uri,
        destination: DocumentFile,
    ) {
        val fileName = getFileNameIfHasNameCollision(destination, name)
        val fileNameWithoutExtension = fileName.substringBeforeLast(".")
        val mimeType = context.contentResolver.getType(source)
            ?: getMimeTypeFromFileName(fileName)
        val newFile = destination.createFile(mimeType, fileNameWithoutExtension)
        newFile?.uri?.let { newUri ->
            context.contentResolver.openInputStream(source)?.use { input ->
                context.contentResolver.openOutputStream(newUri)?.use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun getFileNameIfHasNameCollision(folder: DocumentFile, fileName: String): String {
        val files = folder.listFiles()
        if (files.find { it.name == fileName } == null) return fileName
        val fileNameWithoutExtension = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".", "")
        for (i in 1..Int.MAX_VALUE) {
            val newFileName = if (extension.isNotEmpty()) {
                "$fileNameWithoutExtension ($i).${extension}"
            } else {
                "$fileNameWithoutExtension ($i)"
            }
            if (files.find { it.name == newFileName } == null) return newFileName
        }
        return fileName
    }

    override suspend fun findFileInDirectory(
        directoryPath: UriPath,
        fileNameToFind: String,
    ): DocumentFile? =
        documentFileWrapper.getDocumentFileForSyncContentUri(directoryPath.value)?.listFiles()
            ?.toList()?.find { it.name == fileNameToFind }

    override fun isPathInsecure(path: String): Boolean = path.contains("../")
            || path.contains(APP_PRIVATE_DIR1)
            || path.contains(APP_PRIVATE_DIR2)

    override fun isMalformedPathFromExternalApp(action: String?, path: String): Boolean {
        // Method to check if intent is received from external app with action: ACTION_SEND / ACTION_SEND_MULTIPLE
        val isDataFromExternalApp = action != null &&
                (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE)
        val sanitized = path.replace(" ", "")

        return isDataFromExternalApp && isPathInsecure(sanitized)
    }

    override suspend fun getDocumentEntities(uris: List<Uri>): List<DocumentEntity> {
        return uris.mapNotNull { uri ->
            getDocumentFileFromUri(uri)?.let { doc ->
                val childFiles = if (doc.isDirectory) doc.listFiles() else emptyArray()
                documentFileMapper(
                    file = doc,
                    numFiles = childFiles.count { it.isFile },
                    numFolders = childFiles.count { it.isDirectory },
                ).let {
                    if (documentFileWrapper.isMIUIGalleryRawUri(uri)
                        || documentFileWrapper.isSamsungDeviceWithAndroidLessThanQ()
                    ) {
                        it.copy(uri = UriPath(uri.toString()))
                    } else {
                        it
                    }
                }
            } ?:
            // We cannot get DocumentFile from some special Uris, like HyperOS Security Share.
            // Try using content resolver to get the details.
            getFileNameFromContentResolver(uri)?.let { name ->
                DocumentEntity(
                    name = name,
                    size = getFileSizeFromContentResolver(uri) ?: 0,
                    lastModified = getLastModifiedFromContentResolver(uri),
                    uri = UriPath(uri.toString()),
                )
            }
        }
    }

    private fun getFileNameFromContentResolver(uri: Uri) = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.let { cursor ->
                cursor.use {
                    if (cursor.moveToFirst()) {
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                            ?.let { cursor.getString(it) }
                    } else null
                }
            }
    }.onFailure { Timber.e(it, "Error getting file name from Uri $uri") }.getOrNull()

    private fun getFileSizeFromContentResolver(uri: Uri) = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.let { cursor ->
                cursor.use {
                    if (cursor.moveToFirst()) {
                        cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }
                            ?.let { cursor.getLong(it) }
                    } else null
                }
            }
    }.onFailure { Timber.e(it, "Error getting file size from Uri $uri") }.getOrNull()

    override fun getDocumentMetadataSync(uri: Uri): DocumentMetadata? =
        getDocumentFileFromUri(uri)?.let { doc ->
            DocumentMetadata(doc.name.orEmpty(), doc.isDirectory)
        } ?:
        // We cannot get DocumentFile from some special Uris, like HyperOS Security Share.
        // Try using content resolver to get the file name.
        getFileNameFromContentResolver(uri)?.let { name ->
            DocumentMetadata(name, false)
        }

    override fun getFolderChildUrisSync(uri: Uri): List<Uri> = runCatching {
        getDocumentFileFromUri(uri)
            ?.takeIf { it.isDirectory }
            ?.listFiles()
            ?.map { it.uri }
            ?: emptyList()
    }.getOrDefault(emptyList())

    private fun getDocumentFileFromUri(uri: Uri): DocumentFile? =
        documentFileWrapper.fromUri(uri)

    @RequiresPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
    override suspend fun getFileFromUri(uri: Uri): File? {
        runCatching { getRealPathFromUri(context, uri) }
            .onFailure {
                Timber.e(it, "Failed to get real path from uri")
            }.getOrNull()?.let { path ->
                return File(path).takeIf { it.exists() }
            }
        return null
    }

    override fun getFileDescriptorSync(uriPath: UriPath, writePermission: Boolean) =
        runCatching {
            context.contentResolver.openFileDescriptor(
                uriPath.toUri(),
                if (writePermission) "rw" else "r"
            )
        }.onFailure {
            Timber.w(it, "Error getting file descriptor for $uriPath")
        }.getOrNull()

    override suspend fun getFileDescriptor(
        uriPath: UriPath,
        writePermission: Boolean,
    ) = getFileDescriptorSync(uriPath, writePermission)

    private fun getRealPathFromUri(context: Context, uri: Uri): String? {
        when {
            DocumentsContract.isDocumentUri(context, uri) -> {
                when (uri.authority) {
                    isExternalStorageUri() -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        return Environment.getExternalStorageDirectory()
                            .toString() + "/" + split[1]
                    }

                    isDownloadDocumentUri() -> {
                        var id = DocumentsContract.getDocumentId(uri).orEmpty()
                        if (id.startsWith("raw:")) {
                            return id.substring(4)
                        } else if (id.startsWith("msf:")) {
                            id = id.split(":")[1]
                        }
                        val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentUris.withAppendedId(
                                MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toLong()
                            )
                        } else {
                            ContentUris.withAppendedId(
                                "content://downloads/public_downloads".toUri(),
                                id.toLong()
                            )
                        }
                        return getDataColumn(context, contentUri, null, null)
                    }

                    isMediaDocumentUri() -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        val contentUri = when (split[0]) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> null
                        }
                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])
                        return getDataColumn(context, contentUri, selection, selectionArgs)
                    }
                }
            }

            "content".equals(uri.scheme, ignoreCase = true) -> {
                return getDataColumn(context, uri, null, null)
            }

            "file".equals(uri.scheme, ignoreCase = true) -> {
                return uri.path
            }
        }
        return null
    }

    override suspend fun getInputStream(uriPath: UriPath): InputStream? =
        context.contentResolver.openInputStream(uriPath.toUri())

    override suspend fun canReadUri(stringUri: String) =
        getDocumentFileFromUri(stringUri.toUri())?.canRead() == true

    override fun childFileExistsSync(parentFolder: UriPath, childName: String): Boolean {
        val parentDocumentFile = getDocumentFileFromUri(parentFolder.toUri())
        return parentDocumentFile
            ?.takeIf { it.isDirectory }
            ?.listFiles()
            ?.any { it.name == childName } == true
    }

    /**
     * Get Child File by name
     * @param parentFolder Parent folder
     * @param name Child file name
     */
    override fun getChildByName(parentFolder: UriPath, name: String): UriPath? {
        val parentDocumentFile = getDocumentFileFromUri(parentFolder.toUri())
        return parentDocumentFile
            ?.listFiles()
            ?.firstOrNull { it.name == name }
            ?.let { UriPath(it.uri.toString()) }
    }

    override fun createChildFileSync(
        parentFolder: UriPath,
        childName: String,
        asFolder: Boolean,
    ): UriPath? {
        val parentDocumentFile = getDocumentFileFromUri(parentFolder.toUri())
        return when {
            parentDocumentFile?.isDirectory != true -> null
            asFolder -> parentDocumentFile.createDirectory(childName)
            else -> parentDocumentFile.createFile(getMimeTypeFromFileName(childName), childName)
        }?.let { documentFile ->
            documentFile.let {
                if (documentFile.name != childName) {
                    documentFile.renameTo(childName)
                }

                UriPath(documentFile.uri.toString())
            }
        }
    }

    override fun getParentSync(childUriPath: UriPath): UriPath? {
        val childDocumentFile = getDocumentFileFromUri(childUriPath.toUri())
        return childDocumentFile?.parentFile?.uri?.toString()?.let { UriPath(it) }
    }

    override fun deleteIfItIsAFileSync(uriPath: UriPath): Boolean {
        val documentFile = getDocumentFileFromUri(uriPath.toUri())
        return if (documentFile?.isFile == true) {
            documentFile.delete()
        } else {
            false
        }
    }

    override fun deleteIfItIsAnEmptyFolder(uriPath: UriPath): Boolean {
        val documentFile = getDocumentFileFromUri(uriPath.toUri())
        return if (documentFile?.isDirectory == true && documentFile.listFiles().isEmpty()) {
            documentFile.delete()
        } else {
            false
        }
    }

    override fun setLastModifiedSync(uriPath: UriPath, newTime: Long): Boolean =
        updateFileMTime(uriPath, newTime) || updateDocumentFileMTime(uriPath, newTime)

    private fun updateFileMTime(uriPath: UriPath, newTime: Long): Boolean =
        runCatching {
            setLastModifiedForFile(uriPath.value, newTime)
                ?: getExternalPathByUriSync(uriPath.value)?.let {
                    setLastModifiedForFile(it, newTime)
                }
        }.onFailure {
            Timber.e(it, "Failed to update file mtime")
        }.getOrDefault(false) == true

    private fun setLastModifiedForFile(path: String, newTime: Long) =
        File(path).takeIf { file -> file.exists() }?.setLastModified(newTime)

    private fun updateDocumentFileMTime(uriPath: UriPath, newTime: Long): Boolean {
        val contentValues = ContentValues().apply {
            put(DocumentsContract.Document.COLUMN_LAST_MODIFIED, newTime)
        }
        val updateResult = runCatching {
            context.contentResolver.update(
                uriPath.toUri(),
                contentValues,
                null,
                null
            )
        }.onFailure {
            Timber.e(it, "Failed to update document file mtime")
        }.getOrDefault(0)

        return updateResult > 0
    }

    override fun renameFileSync(uriPath: UriPath, newName: String): UriPath? {
        val documentFile = getDocumentFileFromUri(uriPath.toUri())
        return if (documentFile?.renameTo(newName) == true) {
            UriPath(documentFile.uri.toString())
        } else {
            null
        }
    }

    private fun isMediaDocumentUri() = "com.android.providers.media.documents"

    private fun isDownloadDocumentUri() = "com.android.providers.downloads.documents"

    private fun isExternalStorageUri() = "com.android.externalstorage.documents"

    private fun getMimeTypeFromFileName(fileName: String) =
        getMimeTypeFromExtension(fileName.substringAfterLast(".", ""))

    private fun getMimeTypeFromExtension(extension: String) =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): String? {
        uri?.let {
            context.contentResolver.query(it, arrayOf(DATA), selection, selectionArgs, null)
        }?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(DATA))
            }
        }
        return null
    }

    @SuppressLint("NewApi")
    override suspend fun getFileStorageTypeName(path: String?): FileStorageType {
        val filePath = path ?: return FileStorageType.Unknown
        // Check if the file is in the primary external storage (internal storage)
        val primaryExternalStorage = Environment.getExternalStorageDirectory()
        if (filePath.startsWith(primaryExternalStorage.absolutePath)) {
            return FileStorageType.Internal(deviceGateway.getDeviceModel())
        }
        // Check if the file is on an SD card (removable storage)
        val storageManager = context.getSystemService(StorageManager::class.java)
        storageManager.storageVolumes.forEach { volume ->
            if (volume.isRemovable) {
                // For Android R (API 30) and above, use volume.directory
                if (deviceGateway.getSdkVersionInt() >= Build.VERSION_CODES.R) {
                    volume.directory?.absolutePath?.let { volumePath ->
                        if (filePath.startsWith(volumePath)) {
                            return FileStorageType.SdCard
                        }
                    }
                } else {
                    volume.uuid?.let { uuid ->
                        val possiblePath = "/storage/$uuid"
                        if (filePath.startsWith(possiblePath)) {
                            return FileStorageType.SdCard
                        }
                    }
                    // Additional fallback
                    runCatching {
                        val getPathMethod = StorageVolume::class.java.getMethod("getPath")
                        val volumePath = getPathMethod.invoke(volume) as? String
                        if (volumePath != null && filePath.startsWith(volumePath)) {
                            return FileStorageType.SdCard
                        }
                    }
                }
            }
        }
        // Fallback for older devices: check common SD card paths
        val possibleSdPaths = arrayOf(
            Environment.getExternalStorageDirectory().path,
            "/storage/sdcard1",
            "/mnt/extSdCard",
            "/storage/extSdCard",
            "/storage/removable/sdcard1"
        )
        possibleSdPaths.forEach { path ->
            if (filePath.startsWith(path)) {
                return FileStorageType.SdCard
            }
        }
        return FileStorageType.Unknown
    }

    override suspend fun doesUriPathExist(uriPath: UriPath): Boolean =
        getDocumentFileFromUri(uriPath.toUri())?.exists() == true

    override suspend fun removePersistentPermission(uriPath: UriPath) {
        runCatching {
            val uri = getTreeUriFromDocumentUri(uriPath.value.toUri())
            uri?.let {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Timber.d("Removed persisted permission for uriPath $uriPath and uri $uri")
            }
        }.onFailure {
            Timber.e("Failed to remove persisted permission for $uriPath $it")
        }
    }

    private fun getTreeUriFromDocumentUri(documentUri: Uri): Uri? {
        // "content://com.android.externalstorage.documents/tree/primary%3ADocuments/document/primary%3ADocuments"
        // needs to be converted to content://com.android.externalstorage.documents/tree/primary%3ADocuments
        if (DocumentsContract.isDocumentUri(context, documentUri)
            && DocumentsContract.isTreeUri(documentUri)
        ) {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(documentUri)
            val authority = documentUri.authority
            if (authority != null && treeDocumentId != null) {
                return DocumentsContract.buildTreeDocumentUri(authority, treeDocumentId)
            }
        }
        // If it's not a document URI within a tree, or if components are missing,
        // return the original URI.
        return documentUri
    }

    override fun hasPersistedPermission(uri: Uri, writePermission: Boolean) =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && (if (writePermission) it.isWritePermission else it.isReadPermission)
        }

    override fun takePersistablePermission(uri: Uri, writePermission: Boolean) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            if (writePermission) Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION else Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    @ExperimentalTime
    override suspend fun getLastModifiedTime(uriPath: UriPath) = withContext(Dispatchers.IO) {
        getLastModifiedTimeSync(uriPath)
    }

    @ExperimentalTime
    override fun getLastModifiedTimeSync(uriPath: UriPath) =
        if (uriPath.isPath()) {
            uriPath.value.let { path ->
                File(path).takeIf { file -> file.exists() }?.lastModified()?.takeIf { it > 0 }
                    ?: Files.readAttributes(Paths.get(path), BasicFileAttributes::class.java)
                        .lastModifiedTime().toMillis().takeIf { it > 0 }
            }
        } else {
            uriPath.toUri().let { uri ->
                getDocumentFileFromUri(uri)?.lastModified()?.takeIf { it > 0 }
                    ?: getLastModifiedFromContentResolver(uri).takeIf { it > 0 }
            }
        }?.let { Instant.fromEpochMilliseconds(it) }

    @ExperimentalTime
    override suspend fun getLastModifiedTimeForSyncContentUri(uriPath: UriPath) =
        documentFileWrapper.getDocumentFileForSyncContentUri(uriPath.value)?.lastModified()
            ?.takeIf { it > 0 }
            ?.let { Instant.fromEpochMilliseconds(it) }

    override fun createChildrenFilesSync(
        parentUri: UriPath,
        children: List<String>,
        createIfMissing: Boolean,
        lastAsFolder: Boolean,
    ): UriPath? {
        return createChildrenFiles(parentUri, children, createIfMissing, lastAsFolder).getOrElse {
            Timber.e(it, "Failed to create children files at $parentUri with children: $children")
            null
        }
    }

    private fun createChildrenFiles(
        parentUri: UriPath,
        children: List<String>,
        createIfMissing: Boolean,
        lastAsFolder: Boolean,
    ): Result<UriPath> {
        if (children.isEmpty()) {
            return failure(Exception("Children list cannot be empty"))
        }

        if (children.any { it.isBlank() || it.contains("/") || it.contains("\\") }) {
            return failure(Exception("Invalid characters in path components"))
        }

        var currentDocument = getDocumentFileFromUri(parentUri.toUri())
            ?: return failure(Exception("Invalid parent URI or document not found"))

        if (!currentDocument.isDirectory) {
            return failure(Exception("Parent URI does not point to a directory"))
        }

        for ((index, name) in children.withIndex()) {
            val isLast = index == children.lastIndex
            val shouldBeDirectory = !isLast || lastAsFolder

            var nextDocument: DocumentFile?

            if (createIfMissing) {
                // Create directly
                nextDocument = try {
                    if (shouldBeDirectory) {
                        currentDocument.createDirectory(name)
                    } else {
                        val mimeType = getMimeTypeFromFileName(name)
                        currentDocument.createFile(mimeType, name)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Direct creation failed for '$name'")
                    null
                }

                // If the name does not match, delete and fall back to listFiles
                if (nextDocument != null && !nextDocument.name.equals(name, ignoreCase = true)) {
                    Timber.w("Created file '${nextDocument.name}' does not match expected name '$name'. Deleting it.")
                    try {
                        nextDocument.delete()
                    } catch (e: Exception) {
                        Timber.w(
                            e,
                            "Failed to delete incorrectly named document '${nextDocument.name}'"
                        )
                    }
                    nextDocument = null
                }

                // Find at listFiles
                if (nextDocument == null) {
                    nextDocument = currentDocument.listFiles()
                        .firstOrNull { it.name.equals(name, ignoreCase = true) }
                    if (nextDocument == null) {
                        return failure(Exception("Failed to create or locate '$name'"))
                    }
                }

                // Check type
                if (shouldBeDirectory && !nextDocument.isDirectory) {
                    return failure(Exception("Expected directory but found file: '$name'"))
                }
                if (!shouldBeDirectory && nextDocument.isDirectory) {
                    return failure(Exception("Expected file but found directory: '$name'"))
                }

            } else {
                nextDocument = currentDocument.listFiles()
                    .firstOrNull { it.name.equals(name, ignoreCase = true) }
                    ?: return failure(Exception("Path component '$name' does not exist"))

                if (shouldBeDirectory && !nextDocument.isDirectory) {
                    return failure(Exception("Expected directory but found file: '$name'"))
                }
                if (!shouldBeDirectory && nextDocument.isDirectory) {
                    return failure(Exception("Expected file but found directory: '$name'"))
                }
            }

            currentDocument = nextDocument
        }

        return Result.success(UriPath(currentDocument.uri.toString()))
    }

    private companion object {
        const val DOWNLOAD_DIR = "MEGA Downloads"
        const val PHOTO_DIR = "MEGA Photos"
        const val OFFLINE_DIR = "MEGA Offline"
        const val LAT_LNG = "0/1,0/1,0/1000"
        const val REF_LAT_LNG = "0"
        const val TMP_FOLDER_NAME = "tmp"

        @SuppressLint("SdCardPath")
        const val APP_PRIVATE_DIR1: String = "/data/data/mega.privacy.android.app"

        @SuppressLint("SdCardPath")
        const val APP_PRIVATE_DIR2: String = "/data/user/0/mega.privacy.android.app"
    }
}
