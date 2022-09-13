package mega.privacy.android.domain.usecase

/**
 * Use case for starting a new chat conversation.
 */
fun interface StartConversation {

    /**
     * Invoke.
     *
     * @param isGroup     True if is should create a group chat, false otherwise.
     * @param userHandles List of user handles.
     * @return The chat conversation handle.
     */
    suspend operator fun invoke(isGroup: Boolean, userHandles: List<Long>): Long
}
