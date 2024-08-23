package mega.privacy.android.app.presentation.upload

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.StringsConstants.INVALID_CHARACTERS
import javax.inject.Inject

/**
 * Mapper to get the error message when importing items.
 */
class ImportFilesErrorMessageMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Get the error message when importing items.
     *
     * @param hasWrongName true if any wrong name is present.
     * @param emptyNames Number of empty names.
     * @return The error message.
     */
    operator fun invoke(hasWrongName: Boolean, emptyNames: Int): String = when {
        emptyNames > 0 && hasWrongName -> {
            context.getString(R.string.general_incorrect_names)
        }

        emptyNames > 0 -> {
            context.resources.getQuantityString(R.plurals.empty_names, emptyNames)
        }

        hasWrongName -> {
            context.getString(
                R.string.invalid_characters_defined,
                INVALID_CHARACTERS
            )
        }

        else -> ""
    }
}