package mega.privacy.android.app.zippreview.domain

import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import java.io.*
import java.lang.Exception
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Zip repository implementation class
 */
class ZipFileRepo : IZipFileRepo {

    override suspend fun unzipFile(zipFullPath: String, unZipRootPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            unzip(zipFullPath, unZipRootPath)
        }
    }

    /**
     * Unzip file
     * @param zipFullPath zip file path
     * @param unZipRootPath unzip destination path
     * @return true is unzip succeed.
     */
    fun unzip(zipFullPath: String, unZipRootPath: String): Boolean {
        ZipFile(zipFullPath).apply {
            entries().toList().forEach {
                val zipDestination = File(unZipRootPath + it.name)
                if (it.isDirectory) {
                    if (!zipDestination.exists()) {
                        zipDestination.mkdirs()
                    }
                } else {
                    try {
                        val inputStream = this.getInputStream(it)
                        //Get the parent file. If it is null or
                        // doesn't exist, created the parent folder.
                        val parentFile = zipDestination.parentFile
                        if (parentFile != null) {
                            if (!parentFile.exists()) {
                                parentFile.mkdirs()
                            }
                            val byteArrayOutputStream = ByteArrayOutputStream()
                            val buffer = ByteArray(1024)
                            var count: Int
                            FileOutputStream(zipDestination).use { outputStream ->
                                //Write the file.
                                while (inputStream.read(buffer)
                                        .also { readCount -> count = readCount } != -1
                                ) {
                                    byteArrayOutputStream.write(buffer, 0, count)
                                    val bytes = byteArrayOutputStream.toByteArray()
                                    outputStream.write(bytes)
                                    byteArrayOutputStream.reset()
                                }
                            }
                            inputStream.close()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return false
                    }
                }
            }
        }
        return true
    }

    override suspend fun updateZipInfoList(
        unknownStr: String,
        zipFile: ZipFile,
        folderPath: String
    ): List<ZipInfoBO> {
        return withContext(Dispatchers.IO) {
            updateZipEntries(folderPath, zipFile).map {
                try {
                    zipEntryToZipInfoBO(it, zipFile)
                } catch (e: IllegalArgumentException) {
                    val unknownZipEntry = ZipEntry(unknownStr)
                    ZipInfoBO(
                        zipEntry = unknownZipEntry,
                        zipFileName = unknownZipEntry.name,
                        fileType = FileType.UNKNOWN,
                        info = ""
                    )
                }
            }
        }
    }

    /**
     * Convert ZipEntry to ZipInfoBO
     * @param zipEntry ZipEntry
     * @param zipFile zip file
     * @return ZipInfoBO
     */
    private fun zipEntryToZipInfoBO(zipEntry: ZipEntry, zipFile: ZipFile): ZipInfoBO {
        //If the depth of current element the current depth and
        // the element path is start with the current folder path, add the element.
        return ZipInfoBO(
            zipEntry,
            zipEntry.name,
            fileType = when {
                zipEntry.isDirectory -> FileType.FOLDER
                zipEntry.name.endsWith(SUFFIX_ZIP) -> FileType.ZIP
                else -> FileType.FILE
            },
            info = if (zipEntry.isDirectory)
                countFiles(zipEntry.name, zipFile)
            else
                Util.getSizeString(zipEntry.size)

        )
    }

    /**
     * Updated zip entries when the directory changed
     * @param folderPath folder path
     * @param zipFile ZipFile
     * @return Zip entries
     */
    private fun updateZipEntries(folderPath: String, zipFile: ZipFile): List<ZipEntry> {
        val expectedDepth = if (TextUtils.isEmpty(folderPath)) 1 else folderPath.getPathDepth()
        return zipFile.entries().toList().filter {
            it.name.startsWith(folderPath) and (it.getZipEntryDepth() == expectedDepth)
        }
    }

    /**
     * Count the files number of current folder
     * @param folderPath current folder path
     * @param zipFile Zip file
     * @return files number string of current folder.
     */
    private fun countFiles(folderPath: String, zipFile: ZipFile): String {
        val pathDepth = folderPath.getPathDepth()
        val entryMap = zipFile.entries().toList().filter {
            it.name.startsWith(folderPath) and (it.getZipEntryDepth() >= pathDepth)
        }.groupBy {
            it.isDirectory
        }
        return TextUtil.getFolderInfo(entryMap[true]?.size ?: 0, entryMap[false]?.size ?: 0)
    }

    /**
     * Get depth of current ZipEntry
     * @return depth of current ZipEntry
     */
    private fun ZipEntry.getZipEntryDepth(): Int {
        // directory end with "/" so need to get rid of it
        return name.getPathDepth() - if (isDirectory) 1 else 0
    }

    private fun String.getPathDepth() = count { c -> c == '/' }

    companion object {
        private const val SUFFIX_ZIP = ".zip"
    }
}