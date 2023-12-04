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
        val extension = filePath.substringAfterLast('.')
        val baseName = filePath.substringBeforeLast('.')

        return "$baseName ($index).$extension"
    }
}