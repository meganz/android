package mega.privacy.android.data.model

import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

/**
 * Represents a SDK request events
 */
sealed interface RequestEvent {
    /**
     * Original SDK event
     */
    val request: MegaRequest

    /**
     * OnRequestStart event
     */
    data class OnRequestStart(override val request: MegaRequest) : RequestEvent

    /**
     * OnRequestFinish event
     * @param error the error related to this event, if errorCode is API_OK means the request finished with success
     */
    data class OnRequestFinish(
        override val request: MegaRequest,
        val error: MegaError,
    ) : RequestEvent

    /**
     * OnRequestUpdate event
     */
    data class OnRequestUpdate(override val request: MegaRequest) : RequestEvent

    /**
     * OnRequestTemporaryError event
     * @param error the error related to this event
     */
    data class OnRequestTemporaryError(
        override val request: MegaRequest,
        val error: MegaError,
    ) : RequestEvent

}