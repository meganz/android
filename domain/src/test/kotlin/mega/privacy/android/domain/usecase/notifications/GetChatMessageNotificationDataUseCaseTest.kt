package mega.privacy.android.domain.usecase.notifications

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.NotificationBehaviour
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.notifications.ChatMessageNotificationData
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarColorUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarUseCase
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.domain.usecase.chat.GetMessageSenderNameUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatMessageNotificationDataUseCaseTest {

    private lateinit var underTest: GetChatMessageNotificationDataUseCase

    private val getChatRoomUseCase: GetChatRoomUseCase = mock()
    private val getChatMessageUseCase: GetChatMessageUseCase = mock()
    private val getMessageSenderNameUseCase: GetMessageSenderNameUseCase = mock()
    private val getUserAvatarUseCase: GetUserAvatarUseCase = mock()
    private val getUserAvatarColorUseCase: GetUserAvatarColorUseCase = mock()
    private val getChatMessageNotificationBehaviourUseCase: GetChatMessageNotificationBehaviourUseCase =
        mock()

    private val chatId = -1L
    private val msgId = -2L
    private val userHandle = -3L
    private val defaultSound = "defaultSound"

    @BeforeAll
    fun setup() {
        underTest = GetChatMessageNotificationDataUseCase(
            getChatRoomUseCase,
            getChatMessageUseCase,
            getMessageSenderNameUseCase,
            getUserAvatarUseCase,
            getUserAvatarColorUseCase,
            getChatMessageNotificationBehaviourUseCase,
            UnconfinedTestDispatcher()
        )
    }

    @AfterAll
    fun resetMocks() {
        reset(
            getChatRoomUseCase,
            getChatMessageUseCase,
            getMessageSenderNameUseCase,
            getUserAvatarUseCase,
            getUserAvatarColorUseCase,
            getChatMessageNotificationBehaviourUseCase
        )
    }

    @Test
    fun `test that when a message is deleted a DeletedMessage is returned`() = runTest {
        val chat = mock<ChatRoom>()
        val message = mock<ChatMessage> {
            on { isDeleted }.thenReturn(true)
        }
        val expectedData = ChatMessageNotificationData.DeletedMessage(chat = chat, msg = message)

        whenever(getChatRoomUseCase(chatId)).thenReturn(chat)
        whenever(getChatMessageUseCase(chatId, msgId)).thenReturn(message)

        assertThat(underTest.invoke(true, chatId, msgId, defaultSound)).isEqualTo(expectedData)
    }

    @Test
    fun `test that when a message is seen a SeenMessage is returned`() = runTest {
        val chat = mock<ChatRoom>()
        val message = mock<ChatMessage> {
            on { status } doReturn ChatMessageStatus.SEEN
        }
        val expectedData = ChatMessageNotificationData.SeenMessage(chat = chat, msg = message)

        whenever(getChatRoomUseCase(chatId)).thenReturn(chat)
        whenever(getChatMessageUseCase(chatId, msgId)).thenReturn(message)

        assertThat(underTest.invoke(true, chatId, msgId, defaultSound)).isEqualTo(expectedData)
    }


    @Test
    fun `test that when the message is not deleted nor seen a Message is returned with all the properties`() =
        runTest {
            val shouldBeep = true
            val message = mock<ChatMessage> {
                on { userHandle }.thenReturn(userHandle)
            }
            val chat = mock<ChatRoom>()
            val senderName = "sender"
            val senderAvatar = File("filePath")
            val senderAvatarColor = 125
            val notificationBehaviour = mock<NotificationBehaviour>()
            val expectedData = ChatMessageNotificationData.Message(
                chat = chat,
                msg = message,
                senderName = senderName,
                senderAvatar = senderAvatar,
                senderAvatarColor = senderAvatarColor,
                notificationBehaviour = notificationBehaviour
            )

            whenever(getChatMessageUseCase(chatId, msgId)).thenReturn(message)
            whenever(getChatRoomUseCase(chatId)).thenReturn(chat)
            whenever(getMessageSenderNameUseCase(userHandle, chatId)).thenReturn(senderName)
            whenever(getUserAvatarUseCase(userHandle)).thenReturn(senderAvatar)
            whenever(getUserAvatarColorUseCase(userHandle)).thenReturn(senderAvatarColor)
            whenever(getChatMessageNotificationBehaviourUseCase(shouldBeep, defaultSound))
                .thenReturn(notificationBehaviour)

            assertThat(underTest.invoke(shouldBeep, chatId, msgId, defaultSound)).isEqualTo(
                expectedData
            )
        }
}