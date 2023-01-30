package mega.privacy.android.domain.usecase.setting

/**
 * Use case for checking if chat settings is already initialized. If not, reset them by default.
 */
fun interface ResetChatSettings {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}