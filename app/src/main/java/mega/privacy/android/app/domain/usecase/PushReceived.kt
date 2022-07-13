package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaChatRequest

/**
 * Push received use case.
 */
fun interface PushReceived {

    /**
     * Invoke
     *
     * @param beep   True if should beep, false otherwise.
     * @param chatId Base64-encoded chat identifier.
     * @return Result of the request. Required for creating the notification.
     */
    suspend operator fun invoke(beep: Boolean, chatId: String?): MegaChatRequest
}