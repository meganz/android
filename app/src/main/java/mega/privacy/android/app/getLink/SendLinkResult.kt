package mega.privacy.android.app.getLink

/**
 * Send link result
 *
 * @param chatId Chat id
 */
sealed class SendLinkResult(
    open val chatId: Long,
) {
    /**
     * Normal link
     */
    data class NormalLink(override val chatId: Long) : SendLinkResult(chatId = chatId)

    /**
     * Link with key
     */
    data class LinkWithKey(override val chatId: Long) : SendLinkResult(chatId = chatId)

    /**
     * Link with password
     */
    data class LinkWithPassword(override val chatId: Long) : SendLinkResult(chatId = chatId)
}