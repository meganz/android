package mega.privacy.android.feature.chat.list.formatter

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.core.formatter.emoji.EmojiShortcodeConverter
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.GetChatListItemUseCase
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.domain.usecase.chat.GetMessageSenderNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.time.Duration
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
 * falls back to the genuine-unknown handling (blank content becomes the
 * "unrecognizable" string; otherwise the emoji-converted content, prefixed with
 * the sender in group chats) so previews never regress.
 *
 * Attachment families (node attachments, contact attachments and voice clips)
 * are sender-prefixed and reproduce the legacy chat-list wording: a node
 * attachment shows the first attached node's name, a contact attachment shows the
 * "contacts sent" count, and a voice clip shows the clip duration read from the
 * attached [AudioFileTypeInfo] and rendered as `m:ss`.
 *
 * Contains-meta messages (rich previews, geolocations and giphys) resolve their
 * preview text from [ChatMessage.containsMeta], matching the legacy meta wording.
 *
 * Identity look-ups ([GetMyUserHandleUseCase] and [GetMyFullNameUseCase]) are
 * memoised per formatter instance: each wraps a cheap SDK read in a dispatcher
 * hop, and both are read once per formatted row. Caching a single successful,
 * non-null result avoids repeating that hop; the instance is unscoped and
 * recreated on account switch, so the cache never outlives the signed-in user.
 *
 * Management-message families (participant changes, privilege changes, title
 * changes, history truncation, retention time, and chat-link/private-mode
 * changes) resolve their actor and target names from the underlying
 * [ChatMessage], mirroring the legacy chat-list preview wording.
 *
 * Call and meeting families (started/ongoing calls, ended calls with their
 * duration or termination reason, and scheduled-meeting updates) reproduce the
 * legacy call-preview wording. Call duration is decomposed from the message's
 * [ChatMessage.duration] and rendered with the hour/minute/second plurals; the
 * one-to-one versus group/meeting wording is chosen from the chat room.
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
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val emojiShortcodeConverter: EmojiShortcodeConverter,
) {

    private val identityMutex = Mutex()
    private var cachedMyHandle: Long? = null
    private var cachedMyFullName: String? = null

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

            ChatRoomLastMessage.CallStarted -> callStartedPreview(item.chatId)
            ChatRoomLastMessage.CallEnded -> callEndedPreview(item)
            ChatRoomLastMessage.SchedMeeting -> scheduledMeetingPreview(item)

            ChatRoomLastMessage.NodeAttachment -> nodeAttachmentPreview(item)
            ChatRoomLastMessage.ContactAttachment -> contactAttachmentPreview(item)
            ChatRoomLastMessage.VoiceClip -> voiceClipPreview(item)
            ChatRoomLastMessage.ContainsMeta -> containsMetaPreview(item)

            else -> unknownTypePreview(item)
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

    private suspend fun callStartedPreview(chatId: Long): String =
        if (isMeeting(chatId)) {
            context.getString(sharedR.string.meetings_list_ongoing_call_message)
        } else {
            context.getString(sharedR.string.ongoing_call_messages)
        }

    private suspend fun callEndedPreview(item: ChatListItem): String {
        val message = getChatMessageUseCase(item.chatId, item.lastMessageId) ?: return item.lastMessage
        val chatRoom = runCatching { getChatRoomUseCase(item.chatId) }.getOrNull()
        val isGroup = chatRoom?.isGroup ?: item.isGroup
        val isMeeting = chatRoom?.isMeeting == true
        val body = when (message.termCode) {
            ChatMessageTermCode.ENDED,
            ChatMessageTermCode.BY_MODERATOR,
                -> callEndedDuration(message.duration, isGroup, isMeeting)

            ChatMessageTermCode.REJECTED ->
                stripFormatMarkers(context.getString(sharedR.string.call_rejected_messages))

            ChatMessageTermCode.NO_ANSWER ->
                callUnansweredOrMissed(message.userHandle, sharedR.string.call_not_answered_messages)

            ChatMessageTermCode.CANCELLED ->
                callUnansweredOrMissed(message.userHandle, sharedR.string.call_cancelled_messages)

            ChatMessageTermCode.FAILED ->
                stripFormatMarkers(context.getString(sharedR.string.call_failed_messages))
        }
        return "${resolveName(message.userHandle, item.chatId)}: $body"
    }

    private suspend fun scheduledMeetingPreview(item: ChatListItem): String =
        stripFormatMarkers(
            context.getString(
                sharedR.string.chat_chat_room_message_updated_scheduled_meeting,
                resolveName(item.lastMessageSender, item.chatId),
            )
        )

    private suspend fun nodeAttachmentPreview(item: ChatListItem): String {
        val message = getChatMessageUseCase(item.chatId, item.lastMessageId) ?: return item.lastMessage
        val nodeName = message.nodeList.firstOrNull()?.name
        val content = nodeName?.takeIf { it.isNotBlank() }
            ?: emojiShortcodeConverter.convert(item.lastMessage)
        return "${senderPrefix(item)}: $content"
    }

    private suspend fun contactAttachmentPreview(item: ChatListItem): String {
        val message = getChatMessageUseCase(item.chatId, item.lastMessageId) ?: return item.lastMessage
        val content = emojiShortcodeConverter.convert(
            context.getString(sharedR.string.contacts_sent, message.usersCount.toString())
        )
        return "${senderPrefix(item)}: $content"
    }

    private suspend fun voiceClipPreview(item: ChatListItem): String {
        val message = getChatMessageUseCase(item.chatId, item.lastMessageId) ?: return item.lastMessage
        val duration = (message.nodeList.firstOrNull() as? FileNode)
            ?.type?.let { it as? AudioFileTypeInfo }
            ?.duration
        val content = duration?.let(::voiceClipDuration) ?: VOICE_CLIP_NO_DURATION
        return "${senderPrefix(item)}: $content"
    }

    private suspend fun containsMetaPreview(item: ChatListItem): String {
        val message = getChatMessageUseCase(item.chatId, item.lastMessageId) ?: return item.lastMessage
        val meta = message.containsMeta
        val content = when (meta?.type) {
            ContainsMetaType.RICH_PREVIEW -> meta.textMessage
            ContainsMetaType.GEOLOCATION ->
                context.getString(sharedR.string.title_geolocation_message)

            ContainsMetaType.GIPHY -> meta.giphy?.title.orEmpty()
            else -> return item.lastMessage
        }
        return "${senderPrefix(item)}: $content"
    }

    private suspend fun unknownTypePreview(item: ChatListItem): String {
        if (item.lastMessage.isBlank()) {
            return context.getString(sharedR.string.error_message_unrecognizable)
        }
        val content = emojiShortcodeConverter.convert(item.lastMessage)
        return if (item.isGroup) "${senderPrefix(item)}: $content" else content
    }

    private fun voiceClipDuration(duration: Duration): String =
        duration.toComponents { hours, minutes, seconds, _ ->
            val minutesString = if (minutes < 10) "0$minutes" else "$minutes"
            val secondsString = if (seconds < 10) "0$seconds" else "$seconds"
            val hoursString = when {
                hours <= 0L -> ""
                hours < 10L -> "0$hours:"
                else -> "$hours:"
            }
            "$hoursString$minutesString:$secondsString"
        }

    private suspend fun callUnansweredOrMissed(senderHandle: Long, selfString: Int): String {
        val resId = if (senderHandle == myHandle()) {
            selfString
        } else {
            sharedR.string.call_missed_messages
        }
        return stripFormatMarkers(context.getString(resId))
    }

    private fun callEndedDuration(duration: Duration, isGroup: Boolean, isMeeting: Boolean): String {
        val isOneToOne = !isGroup && !isMeeting
        if (!isOneToOne && duration <= Duration.ZERO) {
            return stripFormatMarkers(
                context.getString(sharedR.string.group_call_ended_no_duration_message)
            )
        }
        val builder = StringBuilder(
            if (isOneToOne) {
                context.getString(sharedR.string.call_ended_message)
            } else {
                context.getString(sharedR.string.group_call_ended_message)
            }
        )
        duration.toComponents { hours, minutes, seconds, _ ->
            if (hours > 0) {
                builder.append(
                    context.resources.getQuantityString(
                        sharedR.plurals.plural_call_ended_messages_hours, hours.toInt(), hours,
                    )
                )
                if (minutes > 0 || seconds > 0) builder.append(", ")
            }
            if (minutes > 0) {
                builder.append(
                    context.resources.getQuantityString(
                        sharedR.plurals.plural_call_ended_messages_minutes, minutes, minutes,
                    )
                )
                if (seconds > 0) builder.append(", ")
            }
            builder.append(
                context.resources.getQuantityString(
                    sharedR.plurals.plural_call_ended_messages_seconds, seconds, seconds,
                )
            )
        }
        return stripFormatMarkers(builder.toString())
    }

    private suspend fun isMeeting(chatId: Long): Boolean =
        runCatching { getChatRoomUseCase(chatId)?.isMeeting }.getOrNull() == true

    private fun stripFormatMarkers(text: String): String =
        text.replace("[A]", "").replace("[/A]", "")
            .replace("[B]", "").replace("[/B]", "")
            .replace("[C]", "").replace("[/C]", "")

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
            myFullName() ?: context.getString(sharedR.string.chat_last_message_sender_me)
        } else {
            runCatching { getMessageSenderNameUseCase(handle, chatId) }
                .getOrNull()?.takeIf { it.isNotBlank() }
                ?: context.getString(sharedR.string.chat_last_message_sender_unknown)
        }

    private suspend fun senderPrefix(item: ChatListItem): String =
        if (item.lastMessageSender == myHandle()) {
            myFullName() ?: context.getString(sharedR.string.chat_last_message_sender_me)
        } else {
            runCatching { getMessageSenderNameUseCase(item.lastMessageSender, item.chatId) }
                .getOrNull()?.takeIf { it.isNotBlank() }
                ?: context.getString(sharedR.string.chat_last_message_sender_unknown)
        }

    private suspend fun myHandle(): Long? = identityMutex.withLock {
        cachedMyHandle ?: runCatching { getMyUserHandleUseCase() }.getOrNull()
            ?.also { cachedMyHandle = it }
    }

    private suspend fun myFullName(): String? = identityMutex.withLock {
        cachedMyFullName ?: runCatching { getMyFullNameUseCase() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.also { cachedMyFullName = it }
    }

    private fun noHistoryPreview(): String =
        context.getString(sharedR.string.chat_last_message_no_history)

    private companion object {
        const val VOICE_CLIP_NO_DURATION = "--:--"
        const val SECONDS_IN_MINUTE = 60L
        const val SECONDS_IN_HOUR = SECONDS_IN_MINUTE * 60
        const val SECONDS_IN_DAY = SECONDS_IN_HOUR * 24
        const val SECONDS_IN_WEEK = SECONDS_IN_DAY * 7
        const val SECONDS_IN_MONTH_30 = SECONDS_IN_DAY * 30
        const val SECONDS_IN_YEAR = SECONDS_IN_DAY * 365
    }
}
