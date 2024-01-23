package mega.privacy.android.app.presentation.versions.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import javax.inject.Inject

/**
 * Mapper implementation for Version History Remove Message
 *
 * @param context as Application Context provided by dependency graph
 */
class VersionHistoryRemoveMessageMapper @Inject constructor(
    @ApplicationContext val context: Context,
) {
    /**
     * Invoke the Mapper
     *
     * @param result as Throwable
     */
    operator fun invoke(result: Throwable?): String = when (result) {
        is VersionsNotDeletedException -> {
            val versionsDeleted =
                result.totalRequestedToDelete - result.totalNotDeleted
            val firstLine = context.getString(
                R.string.version_history_deleted_erroneously
            )
            val secondLine = context.resources.getQuantityString(
                R.plurals.versions_deleted_succesfully,
                versionsDeleted,
                versionsDeleted
            )
            val thirdLine = context.resources.getQuantityString(
                R.plurals.versions_not_deleted,
                result.totalNotDeleted,
                result.totalNotDeleted
            )
            "$firstLine\n$secondLine\n$thirdLine"
        }

        is Throwable -> context.getString(R.string.general_text_error)

        else -> context.getString(R.string.version_history_deleted)

    }
}