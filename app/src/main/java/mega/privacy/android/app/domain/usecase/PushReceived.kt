package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaChatRequest

/**
 * Push received use case.
 */
interface PushReceived {

    /**
     * Invoke
     *
     * @param beep True if should beep, false otherwise.
     * @return Result of the request. Required for creating the notification.
     */
    suspend operator fun invoke(beep: Boolean): MegaChatRequest
}