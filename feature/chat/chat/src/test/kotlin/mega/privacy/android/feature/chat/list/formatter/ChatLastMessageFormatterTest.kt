package mega.privacy.android.feature.chat.list.formatter

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.formatter.emoji.EmojiShortcodeConverter
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.usecase.chat.GetChatListItemUseCase
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.domain.usecase.chat.GetMessageSenderNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.time.Duration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ChatLastMessageFormatterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val getChatListItemUseCase = mock<GetChatListItemUseCase>()
    private val getChatMessageUseCase = mock<GetChatMessageUseCase>()
    private val getMessageSenderNameUseCase = mock<GetMessageSenderNameUseCase>()
    private val getMyUserHandleUseCase = mock<GetMyUserHandleUseCase>()
    private val getMyFullNameUseCase = mock<GetMyFullNameUseCase>()
    private val emojiShortcodeConverter = mock<EmojiShortcodeConverter>()

    private lateinit var underTest: ChatLastMessageFormatter

    @Before
    fun setUp() {
        reset(
            getChatListItemUseCase,
            getChatMessageUseCase,
            getMessageSenderNameUseCase,
            getMyUserHandleUseCase,
            getMyFullNameUseCase,
            emojiShortcodeConverter,
        )
        whenever(emojiShortcodeConverter.convert(any())) doAnswer { it.getArgument(0) }
        underTest = ChatLastMessageFormatter(
            context = context,
            getChatListItemUseCase = getChatListItemUseCase,
            getChatMessageUseCase = getChatMessageUseCase,
            getMessageSenderNameUseCase = getMessageSenderNameUseCase,
            getMyUserHandleUseCase = getMyUserHandleUseCase,
            getMyFullNameUseCase = getMyFullNameUseCase,
            emojiShortcodeConverter = emojiShortcodeConverter,
        )
    }

    @Test
    fun `test that invoke returns an empty string when the chat list item is null`() = runTest {
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn null

        assertThat(underTest(CHAT_ID)).isEmpty()
    }

    @Test
    fun `test that invoke returns the no history preview when the last message type is Invalid`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessageType = ChatRoomLastMessage.Invalid,
            )

            assertThat(underTest(CHAT_ID))
                .isEqualTo(context.getString(sharedR.string.chat_last_message_no_history))
        }

    @Test
    fun `test that invoke returns the no history preview when a normal message has blank content`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "   ",
                lastMessageType = ChatRoomLastMessage.Normal,
            )

            assertThat(underTest(CHAT_ID))
                .isEqualTo(context.getString(sharedR.string.chat_last_message_no_history))
        }

    @Test
    fun `test that invoke returns the plain message without a prefix for a one to one chat`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "Hello there",
                lastMessageType = ChatRoomLastMessage.Normal,
                isGroup = false,
            )

            assertThat(underTest(CHAT_ID)).isEqualTo("Hello there")
        }

    @Test
    fun `test that invoke converts emoji short-codes in the normal message content`() = runTest {
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessage = "Hello :smile:",
            lastMessageType = ChatRoomLastMessage.Normal,
            isGroup = false,
        )
        whenever(emojiShortcodeConverter.convert("Hello :smile:")) doReturn "Hello 😄"

        assertThat(underTest(CHAT_ID)).isEqualTo("Hello 😄")
    }

    @Test
    fun `test that invoke converts the content but not the sender name for a group message`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "Hello :smile:",
                lastMessageType = ChatRoomLastMessage.Normal,
                lastMessageSender = SENDER_HANDLE,
                isGroup = true,
            )
            whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
            whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn ":smile:"
            whenever(emojiShortcodeConverter.convert("Hello :smile:")) doReturn "Hello 😄"

            assertThat(underTest(CHAT_ID)).isEqualTo(":smile:: Hello 😄")
        }

    @Test
    fun `test that invoke prefixes a group message with the resolved sender name`() = runTest {
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessage = "Hello there",
            lastMessageType = ChatRoomLastMessage.Normal,
            lastMessageSender = SENDER_HANDLE,
            isGroup = true,
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo("Alice: Hello there")
    }

    @Test
    fun `test that invoke prefixes a group message with the unknown name when the sender cannot be resolved`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "Hello there",
                lastMessageType = ChatRoomLastMessage.Normal,
                lastMessageSender = SENDER_HANDLE,
                isGroup = true,
            )
            whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
            whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn null

            assertThat(underTest(CHAT_ID)).isEqualTo(
                "${context.getString(sharedR.string.chat_last_message_sender_unknown)}: Hello there"
            )
        }

    @Test
    fun `test that invoke prefixes the current user's own group message with their full name`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "Hello there",
                lastMessageType = ChatRoomLastMessage.Normal,
                lastMessageSender = MY_HANDLE,
                isGroup = true,
            )
            whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
            whenever(getMyFullNameUseCase()) doReturn "My Name"

            assertThat(underTest(CHAT_ID)).isEqualTo("My Name: Hello there")
        }

    @Test
    fun `test that invoke falls back to the me prefix when the current user's full name is blank`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "Hello there",
                lastMessageType = ChatRoomLastMessage.Normal,
                lastMessageSender = MY_HANDLE,
                isGroup = true,
            )
            whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
            whenever(getMyFullNameUseCase()) doReturn ""

            assertThat(underTest(CHAT_ID)).isEqualTo(
                "${context.getString(sharedR.string.chat_last_message_sender_me)}: Hello there"
            )
        }

    @Test
    fun `test that invoke returns the raw last message for a not yet implemented type`() = runTest {
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessage = "raw contact attachment text",
            lastMessageType = ChatRoomLastMessage.ContactAttachment,
            isGroup = true,
        )

        assertThat(underTest(CHAT_ID)).isEqualTo("raw contact attachment text")
    }

    @Test
    fun `test that invoke falls back to the raw message when the management message cannot be resolved`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "raw text",
                lastMessageId = MESSAGE_ID,
                lastMessageType = ChatRoomLastMessage.AlterParticipants,
            )
            whenever(getChatMessageUseCase(CHAT_ID, MESSAGE_ID)) doReturn null

            assertThat(underTest(CHAT_ID)).isEqualTo("raw text")
        }

    @Test
    fun `test that invoke formats an added participant management message`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.AlterParticipants,
            message = managementMessage(
                userHandle = ACTOR_HANDLE,
                handleOfAction = TARGET_HANDLE,
                privilege = ChatRoomPermission.Standard,
            ),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(TARGET_HANDLE, CHAT_ID)) doReturn "Bob"
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(
                sharedR.string.non_format_message_add_participant, "Bob", "Alice",
            )
        )
    }

    @Test
    fun `test that invoke formats a removed participant management message`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.AlterParticipants,
            message = managementMessage(
                userHandle = ACTOR_HANDLE,
                handleOfAction = TARGET_HANDLE,
                privilege = ChatRoomPermission.Removed,
            ),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(TARGET_HANDLE, CHAT_ID)) doReturn "Bob"
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(
                sharedR.string.non_format_message_remove_participant, "Bob", "Alice",
            )
        )
    }

    @Test
    fun `test that invoke formats a participant who left the group chat`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.AlterParticipants,
            message = managementMessage(
                userHandle = TARGET_HANDLE,
                handleOfAction = TARGET_HANDLE,
                privilege = ChatRoomPermission.Removed,
            ),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(TARGET_HANDLE, CHAT_ID)) doReturn "Bob"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(
                sharedR.string.non_format_message_participant_left_group_chat, "Bob",
            )
        )
    }

    @Test
    fun `test that invoke formats a privilege change to host`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.PrivChange,
            message = managementMessage(
                userHandle = ACTOR_HANDLE,
                handleOfAction = TARGET_HANDLE,
                privilege = ChatRoomPermission.Moderator,
            ),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"
        whenever(getMessageSenderNameUseCase(TARGET_HANDLE, CHAT_ID)) doReturn "Bob"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(
                sharedR.string.chat_chats_list_last_message_permissions_changed_to_host,
                "Bob", "Alice",
            )
        )
    }

    @Test
    fun `test that invoke formats a privilege change to standard`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.PrivChange,
            message = managementMessage(
                userHandle = ACTOR_HANDLE,
                handleOfAction = TARGET_HANDLE,
                privilege = ChatRoomPermission.Standard,
            ),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"
        whenever(getMessageSenderNameUseCase(TARGET_HANDLE, CHAT_ID)) doReturn "Bob"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(
                sharedR.string.chat_chats_list_last_message_permissions_changed_to_standard,
                "Bob", "Alice",
            )
        )
    }

    @Test
    fun `test that invoke formats a privilege change to read-only`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.PrivChange,
            message = managementMessage(
                userHandle = ACTOR_HANDLE,
                handleOfAction = TARGET_HANDLE,
                privilege = ChatRoomPermission.ReadOnly,
            ),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"
        whenever(getMessageSenderNameUseCase(TARGET_HANDLE, CHAT_ID)) doReturn "Bob"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(
                sharedR.string.chat_chats_list_last_message_permissions_changed_to_read_only,
                "Bob", "Alice",
            )
        )
    }

    @Test
    fun `test that invoke formats a chat title change without converting emoji`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.ChatTitle,
            message = managementMessage(
                userHandle = ACTOR_HANDLE,
                content = "New :smile: title",
            ),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(
                sharedR.string.non_format_change_title_messages, "Alice", "New :smile: title",
            )
        )
    }

    @Test
    fun `test that invoke formats a truncate management message from another participant`() =
        runTest {
            stubManagementMessage(
                type = ChatRoomLastMessage.Truncate,
                message = managementMessage(userHandle = ACTOR_HANDLE),
            )
            whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
            whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

            assertThat(underTest(CHAT_ID)).isEqualTo(
                context.getString(sharedR.string.non_format_history_cleared_by, "Alice")
            )
        }

    @Test
    fun `test that invoke formats a truncate management message from the current user`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.Truncate,
            message = managementMessage(userHandle = MY_HANDLE),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMyFullNameUseCase()) doReturn "My Name"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(sharedR.string.history_cleared_by, "My Name")
        )
    }

    @Test
    fun `test that invoke formats a retention time change`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.SetRetentionTime,
            message = managementMessage(
                userHandle = ACTOR_HANDLE,
                retentionTime = SECONDS_IN_HOUR,
            ),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        val time = context.resources.getQuantityString(
            sharedR.plurals.subtitle_properties_manage_chat_label_hours, 1, 1,
        )
        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(sharedR.string.retention_history_changed_by, "Alice", time)
        )
    }

    @Test
    fun `test that invoke formats a disabled retention time`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.SetRetentionTime,
            message = managementMessage(
                userHandle = ACTOR_HANDLE,
                retentionTime = 0L,
            ),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(sharedR.string.retention_history_disabled, "Alice")
        )
    }

    @Test
    fun `test that invoke formats a chat link creation management message`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.PublicHandleCreate,
            message = managementMessage(userHandle = ACTOR_HANDLE),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(sharedR.string.message_created_chat_link, "Alice")
        )
    }

    @Test
    fun `test that invoke formats a chat link deletion management message`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.PublicHandleDelete,
            message = managementMessage(userHandle = ACTOR_HANDLE),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(sharedR.string.message_deleted_chat_link, "Alice")
        )
    }

    @Test
    fun `test that invoke formats a set private mode management message`() = runTest {
        stubManagementMessage(
            type = ChatRoomLastMessage.SetPrivateMode,
            message = managementMessage(userHandle = ACTOR_HANDLE),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(sharedR.string.message_set_chat_private, "Alice")
        )
    }

    private suspend fun stubManagementMessage(type: ChatRoomLastMessage, message: ChatMessage) {
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessageId = MESSAGE_ID,
            lastMessageType = type,
            isGroup = true,
        )
        whenever(getChatMessageUseCase(CHAT_ID, MESSAGE_ID)) doReturn message
    }

    private fun managementMessage(
        userHandle: Long = ACTOR_HANDLE,
        handleOfAction: Long = -1L,
        privilege: ChatRoomPermission = ChatRoomPermission.Unknown,
        content: String? = null,
        retentionTime: Long = 0L,
    ) = ChatMessage(
        status = ChatMessageStatus.UNKNOWN,
        messageId = MESSAGE_ID,
        tempId = -1L,
        msgIndex = 0,
        userHandle = userHandle,
        type = ChatMessageType.UNKNOWN,
        hasConfirmedReactions = false,
        timestamp = 0L,
        content = content,
        isEdited = false,
        isDeleted = false,
        isEditable = false,
        isDeletable = false,
        isManagementMessage = true,
        handleOfAction = handleOfAction,
        privilege = privilege,
        code = ChatMessageCode.INVALID_FORMAT,
        usersCount = 0L,
        userHandles = emptyList(),
        userNames = emptyList(),
        userEmails = emptyList(),
        nodeList = emptyList(),
        handleList = emptyList(),
        duration = Duration.ZERO,
        retentionTime = retentionTime,
        termCode = ChatMessageTermCode.ENDED,
        rowId = -1L,
        changes = emptyList(),
        containsMeta = null,
    )

    private companion object {
        const val CHAT_ID = 1L
        const val MESSAGE_ID = 55L
        const val MY_HANDLE = 100L
        const val SENDER_HANDLE = 200L
        const val ACTOR_HANDLE = 300L
        const val TARGET_HANDLE = 400L
        const val SECONDS_IN_HOUR = 3600L
    }
}
