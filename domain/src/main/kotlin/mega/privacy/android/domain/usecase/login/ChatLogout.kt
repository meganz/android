package mega.privacy.android.domain.usecase.login

/**
 * Use case for logging out from chat api.
 */
fun interface ChatLogout {

    /**
     * Invoke.
     *
     * @param disableChatApi Temporary param for disabling megaChatApi.
     */
    suspend operator fun invoke(disableChatApi: DisableChatApi)
}