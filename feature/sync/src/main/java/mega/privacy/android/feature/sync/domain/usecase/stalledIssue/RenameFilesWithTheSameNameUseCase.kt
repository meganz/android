package mega.privacy.android.feature.sync.domain.usecase.stalledIssue

import timber.log.Timber
import java.io.File
import javax.inject.Inject

internal class RenameFilesWithTheSameNameUseCase @Inject constructor() {

    suspend operator fun invoke(filePaths: List<String>) {
        filePaths.drop(1).forEachIndexed { index, filePath ->
            val file = File(filePath)
            val newName = generateNewName(filePath, index + 1)
            val newFile = File(newName)
            file.renameTo(newFile)
        }
    }

    private fun generateNewName(filePath: String, index: Int): String {
        val fileName = filePath.substringAfterLast(File.separator)
        val pathWithoutFileName = filePath.substringBeforeLast(File.separator)
        val fileNameWithoutExtension = fileName.substringBeforeLast(".")
        val extension = filePath.substringAfterLast('.', missingDelimiterValue = "")
        val fullExtension = if (extension.isNotEmpty()) {
            ".$extension"
        } else {
            ""
        }

        return "$pathWithoutFileName${File.separator}$fileNameWithoutExtension ($index)$fullExtension"
    }
}