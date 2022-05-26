package mega.privacy.android.app.domain.usecase

interface PushReceived {

    /**
     * Invoke
     *
     * @param beep True if should beep, false otherwise.
     */
    suspend operator fun invoke(beep: Boolean)
}