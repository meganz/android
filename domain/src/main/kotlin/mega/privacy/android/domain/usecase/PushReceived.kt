package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Push received use case.
 */
fun interface PushReceived {

    /**
     * Invoke
     *
     * @param beep   True if should beep, false otherwise.
     * @return Result of the request. Required for creating the notification.
     */
    suspend operator fun invoke(beep: Boolean): ChatRequest
}