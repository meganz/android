package mega.privacy.android.app.presentation.fileinfo

import mega.privacy.android.app.namecollision.data.NameCollision

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
     * to notify move has finished, either OK or KO
     * @param exception not null if something went wrong
     */
    data class FinishedMoving(val exception: Throwable?) : FileInfoOneOffViewEvent

    /**
     * to notify copy has finished, either OK or KO
     * @param exception not null if something went wrong
     */
    data class FinishedCopying(val exception: Throwable?) : FileInfoOneOffViewEvent

    /**
     * A collision is detected while moving or copying
     * @param collision the name collision detected
     */
    data class CollisionDetected(val collision: NameCollision) : FileInfoOneOffViewEvent
}

