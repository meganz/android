package mega.privacy.android.app.presentation.fileinfo.model

import mega.privacy.android.shared.resources.R as sharedR
import android.content.Context
import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.NameCollision

/**
 * Represents events in the File info screen
 */
sealed interface FileInfoOneOffViewEvent {

    /**
     * we can't do work because there's no Internet connection
     */
    object NotConnected : FileInfoOneOffViewEvent

    /**
     * a not specified error happened
     */
    object GeneralError : FileInfoOneOffViewEvent

    /**
     * Node has been deleted, this screen has no more sense
     */
    object NodeDeleted : FileInfoOneOffViewEvent

    /**
     * Over disk quota has been reached
     */
    object OverDiskQuota : FileInfoOneOffViewEvent

    /**
     * Public link has been copied to the clipboard
     */
    object PublicLinkCopiedToClipboard : FileInfoOneOffViewEvent

    /**
     * A message should be shown
     * @param message the [StringRes] of the message to be shown
     */
    sealed class Message(@StringRes val message: Int) : FileInfoOneOffViewEvent {

        /**
         * Offline file has been removed
         */
        object RemovedOffline : Message(R.string.file_removed_offline)

        /**
         * Node description has been set
         */
        object NodeDescriptionAdded :
            Message(sharedR.string.file_info_information_snackbar_description_added)

        /**
         * Node description has been updated
         */
        object NodeDescriptionUpdated :
            Message(sharedR.string.file_info_information_snackbar_description_updated)
    }

    /**
     * Data class to join all events related to finish moving or copying the node
     * to notify move or copy has finished, either successfully or not
     * @param jobFinished representing the finished job
     * @param exception [Throwable] not null if something went wrong
     */
    data class Finished(
        val jobFinished: FileInfoJobInProgressState,
        val exception: Throwable?,
    ) : FileInfoOneOffViewEvent {
        /**
         * @return the message to be shown in case of failure
         */
        fun failMessage(context: Context) =
            jobFinished.customErrorMessage(context, exception)
                ?: jobFinished.failMessage?.let {
                    context.getString(it)
                }

        /**
         * @return the message to be shown in case of success
         */
        fun successMessage(context: Context) =
            jobFinished.customSuccessMessage(context)
                ?: jobFinished.successMessage?.let {
                    context.getString(it)
                }

    }

    /**
     * A collision is detected while moving or copying
     * @param collision the name collision detected
     */
    data class CollisionDetected(val collision: NameCollision) : FileInfoOneOffViewEvent
}

