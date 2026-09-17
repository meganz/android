package mega.privacy.android.feature.chat.list.formatter

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.formatter.emoji.EmojiShortcodeConverter
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ContainsMeta
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.Giphy
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.GetChatListItemUseCase
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.domain.usecase.chat.GetMessageSenderNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.shared.resources.R as sharedR
import org.mockito.kotlin.stub
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ChatLastMessageFormatterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val getChatListItemUseCase = mock<GetChatListItemUseCase>()
    private val getChatMessageUseCase = mock<GetChatMessageUseCase>()
    private val getMessageSenderNameUseCase = mock<GetMessageSenderNameUseCase>()
    private val getMyUserHandleUseCase = mock<GetMyUserHandleUseCase>()
    private val getMyFullNameUseCase = mock<GetMyFullNameUseCase>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()
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
            getChatRoomUseCase,
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
            getChatRoomUseCase = getChatRoomUseCase,
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
    fun `test that invoke rethrows cancellation instead of falling back to an unknown sender when the coroutine formatting the message is cancelled`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "Hello there",
                lastMessageType = ChatRoomLastMessage.Normal,
                lastMessageSender = SENDER_HANDLE,
                isGroup = true,
            )
            whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
            lateinit var running: Job
            var preview: String? = null
            var thrown: Throwable? = null
            getMessageSenderNameUseCase.stub {
                onBlocking { invoke(SENDER_HANDLE, CHAT_ID) } doAnswer {
                    running.cancel()
                    throw CancellationException("cancelled")
                }
            }

            running = launch {
                thrown = runCatching { preview = underTest(CHAT_ID) }.exceptionOrNull()
            }
            advanceUntilIdle()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
            assertThat(preview).isNull()
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
    fun `test that invoke returns the unrecognizable string when an unknown type has blank content`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "   ",
                lastMessageType = ChatRoomLastMessage.RevokeNodeAttachment,
                isGroup = true,
            )

            assertThat(underTest(CHAT_ID)).isEqualTo(
                context.getString(sharedR.string.error_message_unrecognizable)
            )
        }

    @Test
    fun `test that invoke prefixes an unknown group type with the sender and converted content`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "Some :smile: text",
                lastMessageType = ChatRoomLastMessage.RevokeNodeAttachment,
                lastMessageSender = SENDER_HANDLE,
                isGroup = true,
            )
            whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
            whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn "Alice"
            whenever(emojiShortcodeConverter.convert("Some :smile: text")) doReturn "Some 😄 text"

            assertThat(underTest(CHAT_ID)).isEqualTo("Alice: Some 😄 text")
        }

    @Test
    fun `test that invoke returns the converted content without a prefix for an unknown one to one type`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "Some text",
                lastMessageType = ChatRoomLastMessage.RevokeNodeAttachment,
                isGroup = false,
            )

            assertThat(underTest(CHAT_ID)).isEqualTo("Some text")
        }

    @Test
    fun `test that invoke formats a node attachment with the first node name`() = runTest {
        val node = nodeWithName("holiday.jpg")
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessage = "raw text",
            lastMessageId = MESSAGE_ID,
            lastMessageType = ChatRoomLastMessage.NodeAttachment,
            lastMessageSender = SENDER_HANDLE,
            isGroup = true,
        )
        whenever(getChatMessageUseCase(CHAT_ID, MESSAGE_ID)) doReturn managementMessage(
            userHandle = SENDER_HANDLE,
        ).copy(nodeList = listOf(node))
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo("Alice: holiday.jpg")
    }

    @Test
    fun `test that invoke falls back to the converted last message for a node attachment without nodes`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "attachment :smile:",
                lastMessageId = MESSAGE_ID,
                lastMessageType = ChatRoomLastMessage.NodeAttachment,
                lastMessageSender = SENDER_HANDLE,
                isGroup = true,
            )
            whenever(getChatMessageUseCase(CHAT_ID, MESSAGE_ID)) doReturn managementMessage(
                userHandle = SENDER_HANDLE,
            )
            whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
            whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn "Alice"
            whenever(emojiShortcodeConverter.convert("attachment :smile:")) doReturn "attachment 😄"

            assertThat(underTest(CHAT_ID)).isEqualTo("Alice: attachment 😄")
        }

    @Test
    fun `test that invoke formats a contact attachment with the users count`() = runTest {
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessage = "raw text",
            lastMessageId = MESSAGE_ID,
            lastMessageType = ChatRoomLastMessage.ContactAttachment,
            lastMessageSender = SENDER_HANDLE,
            isGroup = true,
        )
        whenever(getChatMessageUseCase(CHAT_ID, MESSAGE_ID)) doReturn managementMessage(
            userHandle = SENDER_HANDLE,
        ).copy(usersCount = 3L)
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            "Alice: " + context.getString(sharedR.string.contacts_sent, "3")
        )
    }

    @Test
    fun `test that invoke formats a voice clip with its duration`() = runTest {
        val node = voiceClipNode(1.minutes + 3.seconds)
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessage = "raw text",
            lastMessageId = MESSAGE_ID,
            lastMessageType = ChatRoomLastMessage.VoiceClip,
            lastMessageSender = SENDER_HANDLE,
            isGroup = true,
        )
        whenever(getChatMessageUseCase(CHAT_ID, MESSAGE_ID)) doReturn managementMessage(
            userHandle = SENDER_HANDLE,
        ).copy(nodeList = listOf(node))
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo("Alice: 01:03")
    }

    @Test
    fun `test that invoke falls back to a placeholder duration for a voice clip without audio info`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "raw text",
                lastMessageId = MESSAGE_ID,
                lastMessageType = ChatRoomLastMessage.VoiceClip,
                lastMessageSender = SENDER_HANDLE,
                isGroup = true,
            )
            whenever(getChatMessageUseCase(CHAT_ID, MESSAGE_ID)) doReturn managementMessage(
                userHandle = SENDER_HANDLE,
            )
            whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
            whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn "Alice"

            assertThat(underTest(CHAT_ID)).isEqualTo("Alice: --:--")
        }

    @Test
    fun `test that invoke formats a rich preview meta message with its text`() = runTest {
        stubMetaMessage(
            containsMeta(type = ContainsMetaType.RICH_PREVIEW, textMessage = "https://mega.io"),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo("Alice: https://mega.io")
    }

    @Test
    fun `test that invoke formats a geolocation meta message`() = runTest {
        stubMetaMessage(containsMeta(type = ContainsMetaType.GEOLOCATION))
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            "Alice: " + context.getString(sharedR.string.title_geolocation_message)
        )
    }

    @Test
    fun `test that invoke formats a giphy meta message with its title`() = runTest {
        stubMetaMessage(
            containsMeta(
                type = ContainsMetaType.GIPHY,
                giphy = giphy(title = "Happy Friday"),
            ),
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(SENDER_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo("Alice: Happy Friday")
    }

    @Test
    fun `test that invoke falls back to the raw message for a meta message without meta`() = runTest {
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessage = "raw text",
            lastMessageId = MESSAGE_ID,
            lastMessageType = ChatRoomLastMessage.ContainsMeta,
            lastMessageSender = SENDER_HANDLE,
            isGroup = true,
        )
        whenever(getChatMessageUseCase(CHAT_ID, MESSAGE_ID)) doReturn managementMessage(
            userHandle = SENDER_HANDLE,
        )

        assertThat(underTest(CHAT_ID)).isEqualTo("raw text")
    }

    @Test
    fun `test that invoke resolves the identity use cases at most once across multiple invocations`() =
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

            repeat(3) { assertThat(underTest(CHAT_ID)).isEqualTo("My Name: Hello there") }

            verify(getMyUserHandleUseCase, times(1))()
            verify(getMyFullNameUseCase, times(1))()
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

    @Test
    fun `test that invoke formats an ongoing call for a call started message`() = runTest {
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessageType = ChatRoomLastMessage.CallStarted,
        )
        val room = chatRoom(isGroup = false, isMeeting = false)
        whenever(getChatRoomUseCase(CHAT_ID)) doReturn room

        assertThat(underTest(CHAT_ID)).isEqualTo(
            context.getString(sharedR.string.ongoing_call_messages)
        )
    }

    @Test
    fun `test that invoke formats a meeting in progress for a call started message in a meeting`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessageType = ChatRoomLastMessage.CallStarted,
            )
            val room = chatRoom(isGroup = true, isMeeting = true)
            whenever(getChatRoomUseCase(CHAT_ID)) doReturn room

            assertThat(underTest(CHAT_ID)).isEqualTo(
                context.getString(sharedR.string.meetings_list_ongoing_call_message)
            )
        }

    @Test
    fun `test that invoke formats a one to one ended call with its duration`() = runTest {
        stubEndedCall(
            termCode = ChatMessageTermCode.ENDED,
            duration = 5.minutes + 3.seconds,
            isGroup = false,
            isMeeting = false,
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        val expected = "Alice: " + strip(
            context.getString(sharedR.string.call_ended_message) +
                context.resources.getQuantityString(
                    sharedR.plurals.plural_call_ended_messages_minutes, 5, 5,
                ) + ", " +
                context.resources.getQuantityString(
                    sharedR.plurals.plural_call_ended_messages_seconds, 3, 3,
                )
        )
        assertThat(underTest(CHAT_ID)).isEqualTo(expected)
    }

    @Test
    fun `test that invoke formats a group ended call with hours minutes and seconds`() = runTest {
        stubEndedCall(
            termCode = ChatMessageTermCode.BY_MODERATOR,
            duration = 1.hours + 2.minutes + 3.seconds,
            isGroup = true,
            isMeeting = false,
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        val expected = "Alice: " + strip(
            context.getString(sharedR.string.group_call_ended_message) +
                context.resources.getQuantityString(
                    sharedR.plurals.plural_call_ended_messages_hours, 1, 1,
                ) + ", " +
                context.resources.getQuantityString(
                    sharedR.plurals.plural_call_ended_messages_minutes, 2, 2,
                ) + ", " +
                context.resources.getQuantityString(
                    sharedR.plurals.plural_call_ended_messages_seconds, 3, 3,
                )
        )
        assertThat(underTest(CHAT_ID)).isEqualTo(expected)
    }

    @Test
    fun `test that invoke formats a group ended call without a duration`() = runTest {
        stubEndedCall(
            termCode = ChatMessageTermCode.ENDED,
            duration = Duration.ZERO,
            isGroup = true,
            isMeeting = true,
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        val expected = "Alice: " +
            strip(context.getString(sharedR.string.group_call_ended_no_duration_message))
        assertThat(underTest(CHAT_ID)).isEqualTo(expected)
    }

    @Test
    fun `test that invoke formats a rejected call`() = runTest {
        stubEndedCall(termCode = ChatMessageTermCode.REJECTED)
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            "Alice: " + strip(context.getString(sharedR.string.call_rejected_messages))
        )
    }

    @Test
    fun `test that invoke formats a failed call`() = runTest {
        stubEndedCall(termCode = ChatMessageTermCode.FAILED)
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            "Alice: " + strip(context.getString(sharedR.string.call_failed_messages))
        )
    }

    @Test
    fun `test that invoke formats a missed call for the callee when the call was not answered`() =
        runTest {
            stubEndedCall(termCode = ChatMessageTermCode.NO_ANSWER, senderHandle = ACTOR_HANDLE)
            whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
            whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

            assertThat(underTest(CHAT_ID)).isEqualTo(
                "Alice: " + strip(context.getString(sharedR.string.call_missed_messages))
            )
        }

    @Test
    fun `test that invoke formats an unanswered call for the caller`() = runTest {
        stubEndedCall(termCode = ChatMessageTermCode.NO_ANSWER, senderHandle = MY_HANDLE)
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMyFullNameUseCase()) doReturn "My Name"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            "My Name: " + strip(context.getString(sharedR.string.call_not_answered_messages))
        )
    }

    @Test
    fun `test that invoke formats a cancelled call for the caller`() = runTest {
        stubEndedCall(termCode = ChatMessageTermCode.CANCELLED, senderHandle = MY_HANDLE)
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMyFullNameUseCase()) doReturn "My Name"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            "My Name: " + strip(context.getString(sharedR.string.call_cancelled_messages))
        )
    }

    @Test
    fun `test that invoke formats a missed call for the callee when the call was cancelled`() =
        runTest {
            stubEndedCall(termCode = ChatMessageTermCode.CANCELLED, senderHandle = ACTOR_HANDLE)
            whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
            whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

            assertThat(underTest(CHAT_ID)).isEqualTo(
                "Alice: " + strip(context.getString(sharedR.string.call_missed_messages))
            )
        }

    @Test
    fun `test that invoke falls back to the raw message when the ended call cannot be resolved`() =
        runTest {
            whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
                chatId = CHAT_ID,
                lastMessage = "raw text",
                lastMessageId = MESSAGE_ID,
                lastMessageType = ChatRoomLastMessage.CallEnded,
            )
            whenever(getChatMessageUseCase(CHAT_ID, MESSAGE_ID)) doReturn null

            assertThat(underTest(CHAT_ID)).isEqualTo("raw text")
        }

    @Test
    fun `test that invoke formats an updated scheduled meeting message`() = runTest {
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessageId = MESSAGE_ID,
            lastMessageType = ChatRoomLastMessage.SchedMeeting,
            lastMessageSender = ACTOR_HANDLE,
            isGroup = true,
        )
        whenever(getMyUserHandleUseCase()) doReturn MY_HANDLE
        whenever(getMessageSenderNameUseCase(ACTOR_HANDLE, CHAT_ID)) doReturn "Alice"

        assertThat(underTest(CHAT_ID)).isEqualTo(
            strip(
                context.getString(
                    sharedR.string.chat_chat_room_message_updated_scheduled_meeting, "Alice",
                )
            )
        )
    }

    private suspend fun stubEndedCall(
        termCode: ChatMessageTermCode,
        duration: Duration = Duration.ZERO,
        isGroup: Boolean = false,
        isMeeting: Boolean = false,
        senderHandle: Long = ACTOR_HANDLE,
    ) {
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessage = "raw text",
            lastMessageId = MESSAGE_ID,
            lastMessageType = ChatRoomLastMessage.CallEnded,
            isGroup = isGroup,
        )
        whenever(getChatMessageUseCase(CHAT_ID, MESSAGE_ID)) doReturn managementMessage(
            userHandle = senderHandle,
        ).copy(termCode = termCode, duration = duration)
        val room = chatRoom(isGroup, isMeeting)
        whenever(getChatRoomUseCase(CHAT_ID)) doReturn room
    }

    private fun chatRoom(isGroup: Boolean, isMeeting: Boolean): ChatRoom {
        val room = mock<ChatRoom>()
        whenever(room.isGroup) doReturn isGroup
        whenever(room.isMeeting) doReturn isMeeting
        return room
    }

    private fun strip(text: String): String =
        text.replace("[A]", "").replace("[/A]", "")
            .replace("[B]", "").replace("[/B]", "")
            .replace("[C]", "").replace("[/C]", "")

    private suspend fun stubMetaMessage(meta: ContainsMeta) {
        whenever(getChatListItemUseCase(CHAT_ID)) doReturn ChatListItem(
            chatId = CHAT_ID,
            lastMessage = "raw text",
            lastMessageId = MESSAGE_ID,
            lastMessageType = ChatRoomLastMessage.ContainsMeta,
            lastMessageSender = SENDER_HANDLE,
            isGroup = true,
        )
        whenever(getChatMessageUseCase(CHAT_ID, MESSAGE_ID)) doReturn managementMessage(
            userHandle = SENDER_HANDLE,
        ).copy(containsMeta = meta)
    }

    private fun nodeWithName(name: String): Node = mock {
        on { this.name } doReturn name
    }

    private fun voiceClipNode(duration: Duration): FileNode = mock {
        on { type } doReturn AudioFileTypeInfo(
            mimeType = "audio/mp4",
            extension = "m4a",
            duration = duration,
        )
    }

    private fun containsMeta(
        type: ContainsMetaType,
        textMessage: String = "",
        giphy: Giphy? = null,
    ) = ContainsMeta(
        type = type,
        textMessage = textMessage,
        richPreview = null,
        geolocation = null,
        giphy = giphy,
    )

    private fun giphy(title: String) = Giphy(
        mp4Src = null,
        webpSrc = null,
        title = title,
        mp4Size = 0,
        webpSize = 0,
        width = 0,
        height = 0,
    )

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
