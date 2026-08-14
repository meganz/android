package mega.privacy.android.feature.chat.list.formatter

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.usecase.chat.GetChatListItemUseCase
import mega.privacy.android.domain.usecase.chat.GetMessageSenderNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ChatLastMessageFormatterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val getChatListItemUseCase = mock<GetChatListItemUseCase>()
    private val getMessageSenderNameUseCase = mock<GetMessageSenderNameUseCase>()
    private val getMyUserHandleUseCase = mock<GetMyUserHandleUseCase>()
    private val getMyFullNameUseCase = mock<GetMyFullNameUseCase>()

    private lateinit var underTest: ChatLastMessageFormatter

    @Before
    fun setUp() {
        reset(
            getChatListItemUseCase,
            getMessageSenderNameUseCase,
            getMyUserHandleUseCase,
            getMyFullNameUseCase,
        )
        underTest = ChatLastMessageFormatter(
            context = context,
            getChatListItemUseCase = getChatListItemUseCase,
            getMessageSenderNameUseCase = getMessageSenderNameUseCase,
            getMyUserHandleUseCase = getMyUserHandleUseCase,
            getMyFullNameUseCase = getMyFullNameUseCase,
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

    private companion object {
        const val CHAT_ID = 1L
        const val MY_HANDLE = 100L
        const val SENDER_HANDLE = 200L
    }
}
