package mega.privacy.android.domain.usecase

/**
 * Use case for starting a new chat conversation.
 */
fun interface SetOpenInvite {

    /**
     * Invoke.
     *
     * @param chatId     True if is should create a group chat, false otherwise.
     * @param enabled List of user handles.
     * @return The chat conversation handle.
     */
    suspend operator fun invoke(chatId: Long, enabled: Boolean): Long
}