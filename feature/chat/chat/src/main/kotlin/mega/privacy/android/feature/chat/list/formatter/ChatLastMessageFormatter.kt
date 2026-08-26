package mega.privacy.android.feature.chat.list.formatter

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.core.formatter.emoji.EmojiShortcodeConverter
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.usecase.chat.GetChatListItemUseCase
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
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
 * Management-message families (participant changes, privilege changes, title
 * changes, history truncation, retention time, and chat-link/private-mode
 * changes) resolve their actor and target names from the underlying
 * [ChatMessage], mirroring the legacy chat-list preview wording.
 *
 * Emoji short-codes embedded in normal-text message content are converted to
 * their glyphs via [EmojiShortcodeConverter], matching the legacy preview.
 */
internal class ChatLastMessageFormatter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getChatListItemUseCase: GetChatListItemUseCase,
    private val getChatMessageUseCase: GetChatMessageUseCase,
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
            ChatRoomLastMessage.AlterParticipants,
            ChatRoomLastMessage.PrivChange,
            ChatRoomLastMessage.ChatTitle,
            ChatRoomLastMessage.Truncate,
            ChatRoomLastMessage.SetRetentionTime,
            ChatRoomLastMessage.PublicHandleCreate,
            ChatRoomLastMessage.PublicHandleDelete,
            ChatRoomLastMessage.SetPrivateMode,
                -> managementPreview(item)

            else -> item.lastMessage
        }
    }

    private suspend fun plainTextPreview(item: ChatListItem): String {
        if (item.lastMessage.isBlank()) return noHistoryPreview()
        val content = emojiShortcodeConverter.convert(item.lastMessage)
        if (!item.isGroup) return content
        return "${senderPrefix(item)}: $content"
    }

    private suspend fun managementPreview(item: ChatListItem): String {
        val message = getChatMessageUseCase(item.chatId, item.lastMessageId) ?: return item.lastMessage
        return when (item.lastMessageType) {
            ChatRoomLastMessage.AlterParticipants -> alterParticipantsPreview(message, item.chatId)
            ChatRoomLastMessage.PrivChange -> privilegeChangePreview(message, item.chatId)
            ChatRoomLastMessage.ChatTitle -> chatTitlePreview(message, item.chatId)
            ChatRoomLastMessage.Truncate -> truncatePreview(message, item.chatId)
            ChatRoomLastMessage.SetRetentionTime -> retentionTimePreview(message, item.chatId)
            ChatRoomLastMessage.PublicHandleCreate -> publicHandleCreatePreview(message, item.chatId)
            ChatRoomLastMessage.PublicHandleDelete -> publicHandleDeletePreview(message, item.chatId)
            ChatRoomLastMessage.SetPrivateMode -> setPrivateModePreview(message, item.chatId)
            else -> item.lastMessage
        }
    }

    private suspend fun alterParticipantsPreview(message: ChatMessage, chatId: Long): String {
        val targetName = resolveName(message.handleOfAction, chatId)
        val actorName = resolveName(message.userHandle, chatId)
        return when {
            message.privilege != ChatRoomPermission.Removed ->
                context.getString(
                    sharedR.string.non_format_message_add_participant,
                    targetName,
                    actorName,
                )

            message.userHandle == message.handleOfAction ->
                context.getString(
                    sharedR.string.non_format_message_participant_left_group_chat,
                    targetName,
                )

            else -> context.getString(
                sharedR.string.non_format_message_remove_participant,
                targetName,
                actorName,
            )
        }
    }

    private suspend fun privilegeChangePreview(message: ChatMessage, chatId: Long): String {
        val targetName = resolveName(message.handleOfAction, chatId)
        val actorName = resolveName(message.userHandle, chatId)
        val template = when (message.privilege) {
            ChatRoomPermission.Moderator ->
                sharedR.string.chat_chats_list_last_message_permissions_changed_to_host

            ChatRoomPermission.Standard ->
                sharedR.string.chat_chats_list_last_message_permissions_changed_to_standard

            ChatRoomPermission.ReadOnly ->
                sharedR.string.chat_chats_list_last_message_permissions_changed_to_read_only

            else -> return ""
        }
        return context.getString(template, targetName, actorName)
    }

    private suspend fun chatTitlePreview(message: ChatMessage, chatId: Long): String =
        context.getString(
            sharedR.string.non_format_change_title_messages,
            resolveName(message.userHandle, chatId),
            message.content.orEmpty(),
        )

    private suspend fun truncatePreview(message: ChatMessage, chatId: Long): String {
        val actorName = resolveName(message.userHandle, chatId)
        return if (message.userHandle == myHandle()) {
            context.getString(sharedR.string.history_cleared_by, actorName)
        } else {
            context.getString(sharedR.string.non_format_history_cleared_by, actorName)
        }
    }

    private suspend fun retentionTimePreview(message: ChatMessage, chatId: Long): String {
        val actorName = resolveName(message.userHandle, chatId)
        val duration = retentionDuration(message.retentionTime)
        return if (duration == null) {
            context.getString(sharedR.string.retention_history_disabled, actorName)
        } else {
            context.getString(sharedR.string.retention_history_changed_by, actorName, duration)
        }
    }

    private suspend fun publicHandleCreatePreview(message: ChatMessage, chatId: Long): String =
        context.getString(
            sharedR.string.message_created_chat_link,
            resolveName(message.userHandle, chatId),
        )

    private suspend fun publicHandleDeletePreview(message: ChatMessage, chatId: Long): String =
        context.getString(
            sharedR.string.message_deleted_chat_link,
            resolveName(message.userHandle, chatId),
        )

    private suspend fun setPrivateModePreview(message: ChatMessage, chatId: Long): String =
        context.getString(
            sharedR.string.message_set_chat_private,
            resolveName(message.userHandle, chatId),
        )

    private fun retentionDuration(retentionTimeInSeconds: Long): String? = when {
        retentionTimeInSeconds <= 0L -> null
        retentionTimeInSeconds % SECONDS_IN_YEAR == 0L ->
            context.getString(sharedR.string.subtitle_properties_manage_chat_label_year)

        retentionTimeInSeconds % SECONDS_IN_MONTH_30 == 0L ->
            (retentionTimeInSeconds / SECONDS_IN_MONTH_30).toInt().let { months ->
                context.resources.getQuantityString(
                    sharedR.plurals.subtitle_properties_manage_chat_label_months,
                    months,
                    months,
                )
            }

        retentionTimeInSeconds % SECONDS_IN_WEEK == 0L ->
            (retentionTimeInSeconds / SECONDS_IN_WEEK).toInt().let { weeks ->
                context.resources.getQuantityString(
                    sharedR.plurals.subtitle_properties_manage_chat_label_weeks,
                    weeks,
                    weeks,
                )
            }

        retentionTimeInSeconds % SECONDS_IN_DAY == 0L ->
            (retentionTimeInSeconds / SECONDS_IN_DAY).toInt().let { days ->
                context.resources.getQuantityString(
                    sharedR.plurals.label_time_in_days_full,
                    days,
                    days,
                )
            }

        retentionTimeInSeconds % SECONDS_IN_HOUR == 0L ->
            (retentionTimeInSeconds / SECONDS_IN_HOUR).toInt().let { hours ->
                context.resources.getQuantityString(
                    sharedR.plurals.subtitle_properties_manage_chat_label_hours,
                    hours,
                    hours,
                )
            }

        else -> null
    }

    private suspend fun resolveName(handle: Long, chatId: Long): String =
        if (handle == myHandle()) {
            runCatching { getMyFullNameUseCase() }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: context.getString(sharedR.string.chat_last_message_sender_me)
        } else {
            runCatching { getMessageSenderNameUseCase(handle, chatId) }
                .getOrNull()?.takeIf { it.isNotBlank() }
                ?: context.getString(sharedR.string.chat_last_message_sender_unknown)
        }

    private suspend fun senderPrefix(item: ChatListItem): String =
        if (item.lastMessageSender == myHandle()) {
            runCatching { getMyFullNameUseCase() }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: context.getString(sharedR.string.chat_last_message_sender_me)
        } else {
            runCatching { getMessageSenderNameUseCase(item.lastMessageSender, item.chatId) }
                .getOrNull()?.takeIf { it.isNotBlank() }
                ?: context.getString(sharedR.string.chat_last_message_sender_unknown)
        }

    private suspend fun myHandle(): Long? = runCatching { getMyUserHandleUseCase() }.getOrNull()

    private fun noHistoryPreview(): String =
        context.getString(sharedR.string.chat_last_message_no_history)

    private companion object {
        const val SECONDS_IN_MINUTE = 60L
        const val SECONDS_IN_HOUR = SECONDS_IN_MINUTE * 60
        const val SECONDS_IN_DAY = SECONDS_IN_HOUR * 24
        const val SECONDS_IN_WEEK = SECONDS_IN_DAY * 7
        const val SECONDS_IN_MONTH_30 = SECONDS_IN_DAY * 30
        const val SECONDS_IN_YEAR = SECONDS_IN_DAY * 365
    }
}
