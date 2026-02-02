package mega.privacy.android.app.presentation.mapper.file

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.shared.resources.R as SharedR
import javax.inject.Inject

/**
 * Format a folder info in a readable string
 */
class FolderInfoStringMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Format a file info in a readable string
     *
     * @param numFolders including number of folders
     * @param numFiles including number of files
     */
    operator fun invoke(numFolders: Int, numFiles: Int) =
        when {
            numFolders == 0 && numFiles == 0 ->
                context.getString(SharedR.string.empty_file_browser_folder)

            numFolders == 0 && numFiles > 0 ->
                context.resources.getQuantityString(
                    SharedR.plurals.num_of_files_with_parameter,
                    numFiles,
                    numFiles
                )

            numFiles == 0 && numFolders > 0 ->
                context.resources.getQuantityString(
                    SharedR.plurals.num_of_folders_with_parameter,
                    numFolders,
                    numFolders
                )

            else ->
                context.resources.getQuantityString(
                    SharedR.plurals.num_of_folders_and_num_of_files,
                    numFolders,
                    numFolders
                ) + context.resources.getQuantityString(
                    SharedR.plurals.num_of_files_with_parameter,
                    numFiles,
                    numFiles
                )
        }
}