package mega.privacy.android.domain.usecase.login

/**
 * Disables megaChatApi.
 */
fun interface DisableChatApi {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}