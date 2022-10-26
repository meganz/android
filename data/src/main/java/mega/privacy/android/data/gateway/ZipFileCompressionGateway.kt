package mega.privacy.android.data.gateway

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * File compression gateway implements [FileCompressionGateway]
 *
 */
internal class ZipFileCompressionGateway @Inject constructor() : FileCompressionGateway {

    @Throws(AssertionError::class)
    override suspend fun zipFolder(sourceFolder: File, zipFile: File) {
        assert(sourceFolder.isDirectory) { "Only pass directories as the source folder" }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use {
            zipRecursively(it, sourceFolder, "")
        }
    }

    private fun zipRecursively(
        zipOutputStream: ZipOutputStream,
        sourceDirectory: File,
        parentDirectory: String,
    ) {
        val data = ByteArray(2048)

        sourceDirectory.listFiles()?.forEach { f ->
            if (f.isDirectory) {
                zipInternalFolder(parentDirectory, f, zipOutputStream)
            } else {
                zipInternalFile(f, parentDirectory, zipOutputStream, data)
            }
        }

        zipOutputStream.closeEntry()
        zipOutputStream.close()
    }

    private fun zipInternalFolder(
        parentDirectory: String,
        folder: File,
        zipOutputStream: ZipOutputStream,
    ) {
        val path = if (parentDirectory == "") {
            folder.name + File.separator
        } else {
            parentDirectory + File.separator + folder.name + File.separator
        }
        createZipEntry(path, folder, zipOutputStream)
        zipRecursively(zipOutputStream, folder, folder.name)
    }

    private fun zipInternalFile(
        file: File,
        parentDirectory: String,
        zipOutputStream: ZipOutputStream,
        data: ByteArray,
    ) {
        FileInputStream(file).use {
            BufferedInputStream(it).use { stream ->
                val path = parentDirectory + File.separator + file.name
                createZipEntry(path, file, zipOutputStream)
                while (true) {
                    val readBytes = stream.read(data)
                    if (readBytes == -1) {
                        break
                    }
                    zipOutputStream.write(data, 0, readBytes)
                }
            }
        }
    }

    private fun createZipEntry(
        path: String,
        file: File,
        zipOutputStream: ZipOutputStream,
    ) {
        val entry = ZipEntry(path)
        entry.time = file.lastModified()
        entry.size = file.length()
        zipOutputStream.putNextEntry(entry)
    }
}