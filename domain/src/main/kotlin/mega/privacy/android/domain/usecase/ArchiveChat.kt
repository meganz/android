package mega.privacy.android.domain.usecase

/**
 * Archive chat
 */
fun interface ArchiveChat {

    /**
     * Invoke
     *
     * @param chatId
     * @param archive
     */
    suspend operator fun invoke(chatId: Long, archive: Boolean)
}
