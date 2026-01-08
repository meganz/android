package mega.privacy.android.app.presentation.mapper.file

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.core.formatter.formatFileSize
import javax.inject.Inject

/**
 * Format a file size in a readable string
 */
class FileSizeStringMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Format a file size in a readable string
     *
     * @param size the size of the file or folder
     */
    operator fun invoke(size: Long): String = formatFileSize(size, context)
}
