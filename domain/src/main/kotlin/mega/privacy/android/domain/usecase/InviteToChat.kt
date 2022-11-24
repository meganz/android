package mega.privacy.android.domain.usecase

/**
 * Use case for invite to chat
 */
fun interface InviteToChat {

    /**
     * Invoke.
     *
     * @param chatId            The chat id.
     * @param contactsData      List of contacts to add.
     */
    suspend operator fun invoke(chatId: Long, contactsData: List<String>)
}