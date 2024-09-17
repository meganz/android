package mega.privacy.android.data.facade

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
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
import androidx.core.content.FileProvider
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
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.mapper.file.DocumentFileMapper
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.FileNotCreatedException
import mega.privacy.android.domain.exception.NotEnoughStorageException
import nz.mega.sdk.AndroidGfxProcessor
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.Stack
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

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
    ) = kotlin.runCatching {
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
        runCatching { StatFs(rootPath).availableBytes >= file.length() }.onFailure { Timber.e(it) }
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

    override suspend fun getExternalPathByContentUri(contentUri: String): String? = run {
        contentUri
            .toUri()
            .lastPathSegment
            ?.split(":")
            ?.lastOrNull()
            ?.let {
                Environment.getExternalStorageDirectory().path +
                        "/" + it
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
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // use default location for below Android Q
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$PHOTO_DIR"
                )
            }
        }

        val contentResolver = context.contentResolver
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    override suspend fun createNewVideoUri(fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/$PHOTO_DIR"
                )
            }
        }

        val contentResolver = context.contentResolver
        return contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    override suspend fun isFileUri(uriString: String) = uriString.toUri().scheme == "file"

    override suspend fun isFilePath(path: String) = File(path).isFile

    override suspend fun getFileFromUriFile(uriString: String): File =
        uriString.toUri().toFile()

    override suspend fun isContentUri(uriString: String) = uriString.toUri().scheme == "content"

    override suspend fun isDocumentUri(uri: UriPath): Boolean =
        DocumentsContract.isDocumentUri(context, uri.value.toUri())

    override suspend fun isExternalStorageContentUri(uriString: String) =
        with(Uri.parse(uriString)) {
            scheme == "content" && authority?.startsWith("com.android.externalstorage") == true
        }

    override suspend fun getFileNameFromUri(uriString: String): String? {
        val cursor = context.contentResolver.query(uriString.toUri(), null, null, null, null)
        return cursor?.use {
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                    ?.let { cursor.getString(it) }
            } else null
        }
    }

    override suspend fun getFileSizeFromUri(uriString: String): Long? {
        val cursor = context.contentResolver.query(uriString.toUri(), null, null, null, null)
        return cursor?.use {
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }
                    ?.let { cursor.getLong(it) }
            } else null
        }
    }

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

    private fun getLastModifiedFromContentResolver(uri: Uri) =
        context.contentResolver.acquireContentProviderClient(uri)
            ?.use { client ->
                client.query(uri, null, null, null, null)
                    ?.use { cursor ->
                        var lastModified = 0L

                        cursor.moveToFirst()
                        cursor.getColumnIndex(DATE_MODIFIED).takeIf { it != -1 }?.let { index ->
                            lastModified = cursor.getLong(index) * 1000
                        } ?: cursor.getColumnIndex(DATE_ADDED).takeIf { it != -1 }?.let { index ->
                            lastModified = cursor.getLong(index) * 1000
                        } ?: cursor.getColumnIndex(DATE_TAKEN).takeIf { it != -1 }?.let { index ->
                            lastModified = cursor.getLong(index)
                        }

                        lastModified
                    } ?: 0
            } ?: 0

    override fun downscaleImage(file: File, destination: File, maxPixels: Long) {
        val orientation = AndroidGfxProcessor.getExifOrientation(file.absolutePath)
        val fileRect = AndroidGfxProcessor.getImageDimensions(file.absolutePath, orientation)
        val fileBitmap = AndroidGfxProcessor.getBitmap(
            file.absolutePath,
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
                Bitmap.createScaledBitmap(fileBitmap, width, height, true)

            val fOut: FileOutputStream
            try {
                fOut = FileOutputStream(destination)
                scaleBitmap.compress(file.getCompressFormat(), 100, fOut)
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
        val uri = Uri.parse(folder.value)
        val semaphore = Semaphore(10)
        val document = DocumentFile.fromTreeUri(context, uri) ?: throw FileNotFoundException()
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
        val uri = Uri.parse(folder.value)
        val document = DocumentFile.fromTreeUri(context, uri) ?: throw FileNotFoundException()
        stack.addAll(document.listFiles().toList())
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
    private fun File.getCompressFormat(): CompressFormat = when (extension) {
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
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(source.extension)
                ?: "application/octet-stream"
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
        val extension = fileName.substringAfterLast(".", "")
        val mimeType = context.contentResolver.getType(source)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
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

    override suspend fun findFileInDirectory(directoryPath: String, fileNameToFind: String): File? =
        File(directoryPath).listFiles()?.toList()?.find { it.name == fileNameToFind }

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

    private companion object {
        const val DOWNLOAD_DIR = "MEGA Downloads"
        const val PHOTO_DIR = "MEGA Photos"
        const val OFFLINE_DIR = "MEGA Offline"
        const val LAT_LNG = "0/1,0/1,0/1000"
        const val REF_LAT_LNG = "0"

        @SuppressLint("SdCardPath")
        const val APP_PRIVATE_DIR1: String = "/data/data/mega.privacy.android.app"

        @SuppressLint("SdCardPath")
        const val APP_PRIVATE_DIR2: String = "/data/user/0/mega.privacy.android.app"
    }
}
