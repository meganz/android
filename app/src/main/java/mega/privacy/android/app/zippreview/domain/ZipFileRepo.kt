package mega.privacy.android.app.zippreview.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class ZipFileRepo : IZipFileRepo {

    override suspend fun unpackZipFile(zipFullPath: String, unZipRootPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            unpack(zipFullPath, unZipRootPath)
        }
    }

    private fun unpack(zipFullPath: String, unZipRootPath: String): Boolean {
        val fileInputStream: InputStream
        val zipInputStream: ZipInputStream
        try {
            fileInputStream = FileInputStream(zipFullPath)
            zipInputStream = ZipInputStream(BufferedInputStream(fileInputStream))
            var zipEntry: ZipEntry? = null
            zipInputStream.use { inputStream ->
                while (inputStream.nextEntry?.also { zipEntry = it } != null) {
                    zipEntry?.apply {
                        //File unpack path is root path add file path.
                        val unZipPath = File(unZipRootPath + name)
                        //If it's directory, create folder.
                        if (isDirectory) {
                            if (!unZipPath.exists()) {
                                unZipPath.mkdirs()
                            }
                        } else {
                            //Get the parent file. If it is null or
                            // doesn't exist, created the parent folder.
                            val parentFile = unZipPath.parentFile
                            if (parentFile != null) {
                                if (!parentFile.exists()) {
                                    parentFile.mkdirs()
                                }
                                val byteArrayOutputStream = ByteArrayOutputStream()
                                val buffer = ByteArray(1024)
                                var count: Int
                                FileOutputStream(unZipPath).use { outputStream ->
                                    //Write the file.
                                    while (inputStream.read(buffer).also { count = it } != -1) {
                                        byteArrayOutputStream.write(buffer, 0, count)
                                        val bytes = byteArrayOutputStream.toByteArray()
                                        outputStream.write(bytes)
                                        byteArrayOutputStream.reset()
                                    }
                                }
                                inputStream.closeEntry()
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    override suspend fun updateZipInfoList(
        folderPath: String,
        unknownStr: String,
        zipFile: ZipFile
    ): List<ZipInfoBO> {
        return withContext(Dispatchers.IO) {
            updateZipEntries(folderPath, zipFile).map {
                try {
                    getZipInfo(it, zipFile)
                } catch (e: IllegalArgumentException) {
                    val unknownZipEntry = ZipEntry(unknownStr)
                    ZipInfoBO(
                        zipEntry = unknownZipEntry,
                        zipFileName = unknownZipEntry.name,
                        fileType = ZipFileType.UNKNOWN,
                        info = ""
                    )
                }
            }
        }
    }

    private fun getZipInfo(zipEntry: ZipEntry, zipFile: ZipFile): ZipInfoBO {
        //If the depth of current element the current depth and
        // the element path is start with the current folder path, add the element.
        return ZipInfoBO(
            zipEntry,
            zipEntry.name,
            fileType = when {
                zipEntry.isDirectory -> ZipFileType.FOLDER
                zipEntry.name.endsWith(SUFFIX_ZIP) -> ZipFileType.ZIP
                else -> ZipFileType.FILE
            },
            info = if (zipEntry.isDirectory)
                countFiles(zipEntry.name, zipFile)
            else
                Util.getSizeString(zipEntry.size)

        )
    }

    private fun updateZipEntries(folderPath: String, zipFile: ZipFile): List<ZipEntry> {
        val expectedDepth = folderPath.getPathDepth()
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

    private fun ZipEntry.getZipEntryDepth(): Int {
        // directory end with "/" so need to get rid of it
        return name.getPathDepth() - if (isDirectory) 1 else 0
    }

    private fun String.getPathDepth() = count { c -> c == '/' }

    companion object {
        private const val SUFFIX_ZIP = ".zip"
    }
}