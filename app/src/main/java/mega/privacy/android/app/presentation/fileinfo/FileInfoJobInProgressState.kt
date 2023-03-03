package mega.privacy.android.app.presentation.fileinfo

import android.content.Context
import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.extensions.getQuantityStringOrDefault
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import nz.mega.sdk.MegaError

/**
 * Represents the job in progress state
 * @param progressMessage the String resource representing the message to show the progress
 * @param successMessage the String resource representing the message to show in case of success
 * @param failMessage the String resource representing the message to show in case of failure

 */
sealed class FileInfoJobInProgressState(
    @StringRes val progressMessage: Int?,
    @StringRes val successMessage: Int?,
    @StringRes val failMessage: Int?,
) {

    /**
     * [String] to set a custom message if needed
     */
    open fun customErrorMessage(context: Context, exception: Throwable?): String? = null

    /**
     * The node is loading its properties
     */
    object InitialLoading : FileInfoJobInProgressState(null, null, null)

    /**
     * The node is being copied to another folder
     */
    object Copying : FileInfoJobInProgressState(
        progressMessage = R.string.context_copying,
        successMessage = R.string.context_correctly_copied,
        failMessage = R.string.context_no_copied,
    )

    /**
     * The node is being moved to another folder
     */
    object Moving : FileInfoJobInProgressState(
        progressMessage = R.string.context_moving,
        successMessage = R.string.context_correctly_moved,
        failMessage = R.string.context_no_moved,
    )

    /**
     * The node is being moved to the rubbish bin
     */
    object MovingToRubbish : FileInfoJobInProgressState(
        progressMessage = R.string.context_move_to_trash,
        successMessage = R.string.context_correctly_moved,
        failMessage = R.string.context_no_moved,
    )

    /**
     * The node is being deleted
     */
    object Deleting : FileInfoJobInProgressState(
        progressMessage = R.string.context_delete_from_mega,
        successMessage = R.string.context_correctly_removed,
        failMessage = R.string.context_no_removed,
    ) {
        override fun customErrorMessage(context: Context, exception: Throwable?): String? =
            (exception as? MegaException)
                ?.takeIf { it.errorCode == MegaError.API_EMASTERONLY }?.errorMessage
    }

    /**
     * The node's history versions are being deleted
     */
    object DeletingVersions : FileInfoJobInProgressState(
        progressMessage = R.string.delete_versions,
        successMessage = R.string.version_history_deleted,
        failMessage = R.string.version_history_deleted_erroneously
    ) {
        override fun customErrorMessage(context: Context, exception: Throwable?) =
            (exception as? VersionsNotDeletedException)?.let {
                val versionsDeleted = it.totalRequestedToDelete - it.totalNotDeleted
                val firstLine = context.getFormattedStringOrDefault(
                    R.string.version_history_deleted_erroneously
                )
                val secondLine = context.getQuantityStringOrDefault(
                    R.plurals.versions_deleted_succesfully,
                    versionsDeleted,
                    versionsDeleted
                )
                val thirdLine = context.getQuantityStringOrDefault(
                    R.plurals.versions_not_deleted,
                    it.totalNotDeleted,
                    it.totalNotDeleted
                )
                "$firstLine\n$secondLine\n$thirdLine"
            }

    }
}