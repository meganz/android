package mega.privacy.android.domain.usecase

/**
 * Signal chat presence activity
 */
fun interface SignalChatPresenceActivity {

    /**
     * Invoke
     */
    suspend operator fun invoke()
}
