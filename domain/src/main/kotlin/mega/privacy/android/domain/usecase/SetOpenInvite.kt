package mega.privacy.android.domain.usecase


/**
 * Use case for set open invite for chat
 */
fun interface SetOpenInvite {

    /**
     * Invoke.
     *
     * @param chatId  The chat id.
     * @return  True if it's enabled, false if not.
     */
    suspend operator fun invoke(chatId: Long): Boolean
}