package mega.privacy.android.domain.usecase


/**
 * Use case for set open invite for chat
 */
fun interface SetOpenInvite {

    /**
     * Invoke.
     *
     * @param chatId  The chat id.
     * @param enabled  True if allow add participants, false otherwise.
     * @return The chat conversation handle.
     */
    suspend operator fun invoke(chatId: Long, enabled: Boolean): Long
}