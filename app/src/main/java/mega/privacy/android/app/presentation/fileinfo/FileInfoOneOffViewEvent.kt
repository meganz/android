package mega.privacy.android.app.presentation.fileinfo

import android.content.Context
import androidx.annotation.StringRes
import mega.privacy.android.app.R
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
     * Sealed class to join all events related to finish moving or copying the node
     * to notify move or copy has finished, either OK or KO
     * @param successMessage the String resource representing the message to show in case of success
     * @param failMessage the String resource representing the message to show in case of failure
     */
    sealed class Finished(
        @StringRes val successMessage: Int,
        @StringRes private val failMessage: Int,
    ) : FileInfoOneOffViewEvent {

        fun failMessage(context: Context) =
            customErrorMessage ?: context.getFormattedStringOrDefault(failMessage)

        /**
         * [Throwable] not null if something went wrong
         */
        abstract val exception: Throwable?

        /**
         * [String] to set a message from the API if needed
         */
        open val customErrorMessage: String? = null

        /**
         * to notify copy has finished, either OK or KO
         * @param exception not null if something went wrong
         */
        data class Copying(override val exception: Throwable?) : Finished(
            successMessage = R.string.context_correctly_copied,
            failMessage = R.string.context_no_copied,
        )

        /**
         * to notify move has finished, either OK or KO
         * @param exception not null if something went wrong
         */
        data class Moving(override val exception: Throwable?) : Finished(
            successMessage = R.string.context_correctly_moved,
            failMessage = R.string.context_no_moved,
        )

        /**
         * to notify move to rubbish bin has finished, either OK or KO
         * @param exception not null if something went wrong
         */
        data class MovingToRubbish(override val exception: Throwable?) : Finished(
            successMessage = R.string.context_correctly_moved,
            failMessage = R.string.context_no_moved,
        )

        /**
         * to notify deleting the node has finished, either OK or KO
         * @param exception not null if something went wrong
         * @param customErrorMessage to set a message from the API if needed
         */
        data class Deleting(
            override val exception: Throwable?,
            override val customErrorMessage: String?,
        ) : Finished(
            successMessage = R.string.context_correctly_removed,
            failMessage = R.string.context_no_removed,
        )

    }

    /**
     * A collision is detected while moving or copying
     * @param collision the name collision detected
     */
    data class CollisionDetected(val collision: NameCollision) : FileInfoOneOffViewEvent
}

