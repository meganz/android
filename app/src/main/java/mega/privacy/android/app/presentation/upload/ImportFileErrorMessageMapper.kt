package mega.privacy.android.app.presentation.upload

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import javax.inject.Inject

/**
 * Mapper to get the error message when importing a file.
 */
class ImportFileErrorMessageMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Get the error message when importing a file.
     *
     * @param fileName Name of the file.
     * @return The error message.
     */
    operator fun invoke(fileName: String): String? = when {
        fileName.isBlank() -> {
            context.getString(R.string.empty_name)
        }

        Constants.NODE_NAME_REGEX.matcher(fileName).find() -> {
            context.getString(R.string.invalid_characters)
        }

        else -> null
    }
}