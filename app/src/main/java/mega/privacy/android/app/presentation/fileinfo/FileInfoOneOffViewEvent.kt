package mega.privacy.android.app.presentation.fileinfo

import android.content.Context
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault

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
                    context.getFormattedStringOrDefault(it)
                }

    }

    /**
     * A collision is detected while moving or copying
     * @param collision the name collision detected
     */
    data class CollisionDetected(val collision: NameCollision) : FileInfoOneOffViewEvent
}

