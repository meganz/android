package mega.privacy.android.app.presentation.upload

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.StringsConstants.INVALID_CHARACTERS
import mega.privacy.android.shared.resources.R as sharedR
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
     * @param hasInvalidCharsNames true if any name contains invalid characters.
     * @param hasDotNames true if any name contains only a dot.
     * @param hasDoubleDotNames true if any name contains only a double dot.
     * @param emptyNames Number of empty names.
     * @return The error message.
     */
    operator fun invoke(
        hasInvalidCharsNames: Boolean,
        hasDotNames: Boolean,
        hasDoubleDotNames: Boolean,
        emptyNames: Int,
    ): String = when {
        (emptyNames > 0 && (hasInvalidCharsNames || hasDotNames || hasDoubleDotNames))
                || (hasDotNames && (hasDoubleDotNames || hasInvalidCharsNames))
                || hasDoubleDotNames && hasInvalidCharsNames -> {
            context.getString(R.string.general_incorrect_names)
        }

        emptyNames > 0 -> {
            context.resources.getQuantityString(R.plurals.empty_names, emptyNames)
        }

        hasDotNames -> {
            context.getString(sharedR.string.general_invalid_dot_name_warning)
        }

        hasDoubleDotNames -> {
            context.getString(sharedR.string.general_invalid_double_dot_name_warning)
        }

        hasInvalidCharsNames -> {
            context.getString(
                sharedR.string.general_invalid_characters_defined,
                INVALID_CHARACTERS
            )
        }

        else -> ""
    }
}