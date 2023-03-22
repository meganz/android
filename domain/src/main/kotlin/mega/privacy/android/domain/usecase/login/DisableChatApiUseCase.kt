package mega.privacy.android.domain.usecase.login

/**
 * Disables megaChatApi.
 */
fun interface DisableChatApiUseCase {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}