package mega.privacy.android.feature.chat.list.formatter

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.core.formatter.emoji.EmojiShortcodeConverter
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.usecase.chat.GetChatListItemUseCase
import mega.privacy.android.domain.usecase.chat.GetMessageSenderNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Builds the localised last-message preview shown for a chat room in the chat list.
 *
 * Structured facts are sourced from `:domain` use cases; localisation into
 * strings and plurals happens here because `:domain` is a pure JVM module and
 * cannot resolve Android resources.
 *
 * The dispatch in [invoke] is keyed by [ChatListItem.lastMessageType]: each
 * message family is a dedicated branch, and any type without a dedicated branch
 * falls back to the raw last-message text so previews never regress.
 *
 * Emoji short-codes embedded in normal-text message content are converted to
 * their glyphs via [EmojiShortcodeConverter], matching the legacy preview.
 */
internal class ChatLastMessageFormatter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getChatListItemUseCase: GetChatListItemUseCase,
    private val getMessageSenderNameUseCase: GetMessageSenderNameUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val getMyFullNameUseCase: GetMyFullNameUseCase,
    private val emojiShortcodeConverter: EmojiShortcodeConverter,
) {

    /**
     * Format the last-message preview for the given chat.
     *
     * @param chatId Chat room identifier.
     * @return The preview text, or an empty string when the chat cannot be resolved.
     */
    suspend operator fun invoke(chatId: Long): String {
        val item = getChatListItemUseCase(chatId) ?: return ""
        return when (item.lastMessageType) {
            ChatRoomLastMessage.Invalid -> noHistoryPreview()
            ChatRoomLastMessage.Normal -> plainTextPreview(item)
            else -> item.lastMessage
        }
    }

    private suspend fun plainTextPreview(item: ChatListItem): String {
        if (item.lastMessage.isBlank()) return noHistoryPreview()
        val content = emojiShortcodeConverter.convert(item.lastMessage)
        if (!item.isGroup) return content
        return "${senderPrefix(item)}: $content"
    }

    private suspend fun senderPrefix(item: ChatListItem): String =
        if (item.lastMessageSender == runCatching { getMyUserHandleUseCase() }.getOrNull()) {
            runCatching { getMyFullNameUseCase() }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: context.getString(sharedR.string.chat_last_message_sender_me)
        } else {
            runCatching { getMessageSenderNameUseCase(item.lastMessageSender, item.chatId) }
                .getOrNull()?.takeIf { it.isNotBlank() }
                ?: context.getString(sharedR.string.chat_last_message_sender_unknown)
        }

    private fun noHistoryPreview(): String =
        context.getString(sharedR.string.chat_last_message_no_history)
}
