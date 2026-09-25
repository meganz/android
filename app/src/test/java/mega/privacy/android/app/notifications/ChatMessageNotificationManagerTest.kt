package mega.privacy.android.app.notifications

import android.app.Notification
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.NotificationBehaviour
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.notifications.ChatMessageNotificationData
import nz.mega.sdk.MegaApiJava
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ChatMessageNotificationManagerTest {

    private lateinit var underTest: ChatMessageNotificationManager

    private lateinit var context: Context
    private val notificationManagerCompat = mock<NotificationManagerCompat>()
    private val chatManagement = mock<ChatManagement>()
    private val fileDurationMapper = mock<FileDurationMapper>()

    private val chat = mock<ChatRoom> {
        on { chatId } doReturn CHAT_ID
        on { title } doReturn CHAT_TITLE
        on { unreadCount } doReturn 1
        on { isGroup } doReturn false
    }
    private val message = mock<ChatMessage> {
        on { messageId } doReturn MESSAGE_ID
        on { userHandle } doReturn SENDER_HANDLE
        on { type } doReturn ChatMessageType.NORMAL
        on { content } doReturn INCOMING_TEXT
        on { timestamp } doReturn 1L
    }
    private val data = ChatMessageNotificationData.Message(
        chat = chat,
        msg = message,
        senderName = SENDER_NAME,
        senderAvatar = null,
        senderAvatarColor = Color.RED,
        notificationBehaviour = NotificationBehaviour(),
        myAvatar = null,
        myAvatarColor = Color.BLUE,
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        whenever(chatManagement.openChatId).thenReturn(MegaApiJava.INVALID_HANDLE)
        whenever(notificationManagerCompat.activeNotifications).thenReturn(emptyList())
        underTest = ChatMessageNotificationManager(
            notificationManagerCompat = notificationManagerCompat,
            chatManagement = chatManagement,
        )
    }

    @Test
    fun `test that show posts a single conversation notification per chat`() = runTest {
        withBase64HandleStubbed {
            underTest.show(context, data, fileDurationMapper)
        }

        verify(notificationManagerCompat, times(1)).notify(any(), any())
        verify(notificationManagerCompat).notify(eq(CONVERSATION_NOTIFICATION_ID), any())
    }

    @Test
    fun `test that show adds a reply action with a remote input to the conversation notification`() =
        runTest {
            withBase64HandleStubbed {
                underTest.show(context, data, fileDurationMapper)
            }

            verify(notificationManagerCompat).notify(
                eq(CONVERSATION_NOTIFICATION_ID),
                argThat { notification: Notification ->
                    notification.actions.orEmpty().any { action ->
                        action.remoteInputs.orEmpty().any { remoteInput ->
                            remoteInput.resultKey == ChatMessageReplyReceiver.KEY_TEXT_REPLY
                        }
                    }
                }
            )
        }

    @Test
    fun `test that show gives the current user an own avatar icon in the messaging style`() =
        runTest {
            withBase64HandleStubbed {
                underTest.show(context, data, fileDurationMapper)
            }

            verify(notificationManagerCompat).notify(
                eq(CONVERSATION_NOTIFICATION_ID),
                argThat { notification: Notification ->
                    val user = NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification)
                        ?.user
                    user?.icon != null &&
                            user.name == context.getString(R.string.chat_last_message_sender_me)
                }
            )
        }

    @Test
    fun `test that show stores the message id of the new conversation`() = runTest {
        withBase64HandleStubbed {
            underTest.show(context, data, fileDurationMapper)
        }

        verify(notificationManagerCompat).notify(
            eq(CONVERSATION_NOTIFICATION_ID),
            argThat { notification: Notification ->
                notification.extras.getLongArray(ChatMessageNotificationManager.EXTRA_MESSAGE_IDS)
                    ?.toList() == listOf(MESSAGE_ID)
            }
        )
    }

    @Test
    fun `test that show appends the message to the conversation already shown`() = runTest {
        whenever(notificationManagerCompat.activeNotifications).thenReturn(
            listOf(
                shownNotification(
                    messages = listOf(EARLIER_TEXT to EARLIER_MESSAGE_ID, REPLY_TEXT to MegaApiJava.INVALID_HANDLE),
                )
            )
        )

        withBase64HandleStubbed {
            underTest.show(context, data, fileDurationMapper)
        }

        verify(notificationManagerCompat).notify(
            eq(CONVERSATION_NOTIFICATION_ID),
            argThat { notification: Notification ->
                val style = NotificationCompat.MessagingStyle
                    .extractMessagingStyleFromNotification(notification)
                val texts = style?.messages.orEmpty().map { it.text.toString() }
                val ids = notification.extras
                    .getLongArray(ChatMessageNotificationManager.EXTRA_MESSAGE_IDS)?.toList()
                texts == listOf(EARLIER_TEXT, REPLY_TEXT, INCOMING_TEXT) &&
                        ids == listOf(EARLIER_MESSAGE_ID, MegaApiJava.INVALID_HANDLE, MESSAGE_ID) &&
                        style?.messages?.last()?.person?.name == SENDER_NAME
            }
        )
    }

    @Test
    fun `test that show replaces the message when it is delivered to the conversation again`() =
        runTest {
            whenever(notificationManagerCompat.activeNotifications).thenReturn(
                listOf(
                    shownNotification(
                        messages = listOf(
                            EARLIER_TEXT to EARLIER_MESSAGE_ID,
                            "Old text" to MESSAGE_ID,
                            REPLY_TEXT to MegaApiJava.INVALID_HANDLE,
                        ),
                    )
                )
            )

            withBase64HandleStubbed {
                underTest.show(context, data, fileDurationMapper)
            }

            verify(notificationManagerCompat).notify(
                eq(CONVERSATION_NOTIFICATION_ID),
                argThat { notification: Notification ->
                    val style = NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification)
                    val texts = style?.messages.orEmpty().map { it.text.toString() }
                    val ids = notification.extras
                        .getLongArray(ChatMessageNotificationManager.EXTRA_MESSAGE_IDS)?.toList()
                    texts == listOf(EARLIER_TEXT, INCOMING_TEXT, REPLY_TEXT) &&
                            ids == listOf(EARLIER_MESSAGE_ID, MESSAGE_ID, MegaApiJava.INVALID_HANDLE) &&
                            style?.messages?.get(1)?.person?.name == SENDER_NAME
                }
            )
        }

    @Test
    fun `test that show lets a new incoming message alert again`() = runTest {
        whenever(notificationManagerCompat.activeNotifications).thenReturn(
            listOf(shownNotification(messages = listOf(EARLIER_TEXT to EARLIER_MESSAGE_ID)))
        )

        withBase64HandleStubbed {
            underTest.show(context, data, fileDurationMapper)
        }

        verify(notificationManagerCompat).notify(
            eq(CONVERSATION_NOTIFICATION_ID),
            argThat { notification: Notification ->
                (notification.flags and Notification.FLAG_ONLY_ALERT_ONCE) == 0
            }
        )
    }

    @Test
    fun `test that show does not alert again when the same message is delivered again`() =
        runTest {
            whenever(notificationManagerCompat.activeNotifications).thenReturn(
                listOf(shownNotification(messages = listOf(INCOMING_TEXT to MESSAGE_ID)))
            )

            withBase64HandleStubbed {
                underTest.show(context, data, fileDurationMapper)
            }

            verify(notificationManagerCompat).notify(
                eq(CONVERSATION_NOTIFICATION_ID),
                argThat { notification: Notification ->
                    (notification.flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0
                }
            )
        }

    @Test
    fun `test that show marks a group chat as a group conversation with the sender on the message`() =
        runTest {
            val groupChat = mock<ChatRoom> {
                on { chatId } doReturn CHAT_ID
                on { title } doReturn CHAT_TITLE
                on { unreadCount } doReturn 1
                on { isGroup } doReturn true
            }

            withBase64HandleStubbed {
                underTest.show(context, data.copy(chat = groupChat), fileDurationMapper)
            }

            verify(notificationManagerCompat).notify(
                eq(CONVERSATION_NOTIFICATION_ID),
                argThat { notification: Notification ->
                    val style = NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification)
                    style?.isGroupConversation == true &&
                            style.messages.last().person?.icon != null
                }
            )
        }

    @Test
    fun `test that show leaves the sender icon out in a one to one chat`() = runTest {
        withBase64HandleStubbed {
            underTest.show(context, data, fileDurationMapper)
        }

        verify(notificationManagerCompat).notify(
            eq(CONVERSATION_NOTIFICATION_ID),
            argThat { notification: Notification ->
                val style = NotificationCompat.MessagingStyle
                    .extractMessagingStyleFromNotification(notification)
                style?.isGroupConversation == false &&
                        style.messages.last().person?.icon == null
            }
        )
    }

    @Test
    fun `test that show survives the active notifications not being readable`() = runTest {
        whenever(notificationManagerCompat.activeNotifications)
            .thenThrow(RuntimeException("dead system"))

        withBase64HandleStubbed {
            underTest.show(context, data, fileDurationMapper)
        }

        verify(notificationManagerCompat).notify(eq(CONVERSATION_NOTIFICATION_ID), any())
    }

    @Test
    fun `test that show cancels the conversation notification when the chat is already open`() =
        runTest {
            whenever(chatManagement.openChatId).thenReturn(CHAT_ID)

            withBase64HandleStubbed {
                underTest.show(context, data, fileDurationMapper)
            }

            verify(notificationManagerCompat).cancel(CONVERSATION_NOTIFICATION_ID)
            verify(notificationManagerCompat, never()).notify(any(), any())
        }

    @Test
    fun `test that show removes a deleted message from the conversation without alerting`() =
        runTest {
            whenever(notificationManagerCompat.activeNotifications).thenReturn(
                listOf(
                    shownNotification(
                        messages = listOf(EARLIER_TEXT to EARLIER_MESSAGE_ID, INCOMING_TEXT to MESSAGE_ID),
                    )
                )
            )

            withBase64HandleStubbed {
                underTest.show(
                    context,
                    ChatMessageNotificationData.DeletedMessage(chat = chat, msg = message),
                    fileDurationMapper,
                )
            }

            verify(notificationManagerCompat).notify(
                eq(CONVERSATION_NOTIFICATION_ID),
                argThat { notification: Notification ->
                    val texts = NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification)
                        ?.messages.orEmpty().map { it.text.toString() }
                    val ids = notification.extras
                        .getLongArray(ChatMessageNotificationManager.EXTRA_MESSAGE_IDS)?.toList()
                    texts == listOf(EARLIER_TEXT) &&
                            ids == listOf(EARLIER_MESSAGE_ID) &&
                            (notification.flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0
                }
            )
        }

    @Test
    fun `test that show cancels the conversation when the deleted message was the last incoming one`() =
        runTest {
            whenever(notificationManagerCompat.activeNotifications).thenReturn(
                listOf(
                    shownNotification(
                        messages = listOf(INCOMING_TEXT to MESSAGE_ID, REPLY_TEXT to MegaApiJava.INVALID_HANDLE),
                    )
                )
            )

            withBase64HandleStubbed {
                underTest.show(
                    context,
                    ChatMessageNotificationData.DeletedMessage(chat = chat, msg = message),
                    fileDurationMapper,
                )
            }

            verify(notificationManagerCompat).cancel(CONVERSATION_NOTIFICATION_ID)
            verify(notificationManagerCompat, never()).notify(any(), any())
        }

    @Test
    fun `test that show cancels the conversation when a seen message leaves nothing unread`() =
        runTest {
            val readChat = mock<ChatRoom> {
                on { chatId } doReturn CHAT_ID
                on { unreadCount } doReturn 0
            }

            withBase64HandleStubbed {
                underTest.show(
                    context,
                    ChatMessageNotificationData.SeenMessage(chat = readChat, msg = message),
                    fileDurationMapper,
                )
            }

            verify(notificationManagerCompat).cancel(CONVERSATION_NOTIFICATION_ID)
            verify(notificationManagerCompat, never()).notify(any(), any())
        }

    @Test
    fun `test that appendReply posts the shown notification again with the reply appended`() {
        whenever(notificationManagerCompat.activeNotifications)
            .thenReturn(listOf(shownNotification(messages = listOf(INCOMING_TEXT to MESSAGE_ID))))

        underTest.appendReply(context, CONVERSATION_NOTIFICATION_ID, REPLY_TEXT)

        verify(notificationManagerCompat).notify(
            eq(CONVERSATION_NOTIFICATION_ID),
            argThat { notification: Notification ->
                val style = NotificationCompat.MessagingStyle
                    .extractMessagingStyleFromNotification(notification)
                val messages = style?.messages.orEmpty()
                val ids = notification.extras
                    .getLongArray(ChatMessageNotificationManager.EXTRA_MESSAGE_IDS)?.toList()
                messages.map { it.text.toString() } == listOf(INCOMING_TEXT, REPLY_TEXT) &&
                        messages.last().person?.name == style?.user?.name &&
                        ids == listOf(MESSAGE_ID, MegaApiJava.INVALID_HANDLE)
            }
        )
    }

    @Test
    fun `test that appendReply keeps the reply action when posting the notification again`() {
        whenever(notificationManagerCompat.activeNotifications)
            .thenReturn(listOf(shownNotification(messages = listOf(INCOMING_TEXT to MESSAGE_ID))))

        underTest.appendReply(context, CONVERSATION_NOTIFICATION_ID, REPLY_TEXT)

        verify(notificationManagerCompat).notify(
            eq(CONVERSATION_NOTIFICATION_ID),
            argThat { notification: Notification ->
                notification.actions.orEmpty().any { action ->
                    action.remoteInputs.orEmpty().isNotEmpty()
                }
            }
        )
    }

    @Test
    fun `test that appendReply posts nothing when the notification is no longer shown`() {
        underTest.appendReply(context, CONVERSATION_NOTIFICATION_ID, REPLY_TEXT)

        verify(notificationManagerCompat, never()).notify(any(), any())
    }

    /**
     * A conversation notification as [ChatMessageNotificationManager] would have posted it.
     *
     * @param messages Text and chat message id of every message, replies use [MegaApiJava.INVALID_HANDLE]
     */
    private fun shownNotification(messages: List<Pair<String, Long>>): StatusBarNotification {
        val me = Person.Builder().setName("Me").setKey("me").build()
        val sender = Person.Builder().setName(SENDER_NAME).setKey(SENDER_HANDLE.toString()).build()
        val style = NotificationCompat.MessagingStyle(me).setConversationTitle(CHAT_TITLE)
        messages.forEachIndexed { index, (text, messageId) ->
            val person = if (messageId == MegaApiJava.INVALID_HANDLE) me else sender
            style.addMessage(text, index.toLong(), person)
        }
        val notification =
            NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_CHAT_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setStyle(style)
                .addAction(
                    NotificationCompat.Action.Builder(
                        android.R.drawable.ic_dialog_info,
                        "Reply",
                        ChatMessageReplyReceiver.getPendingIntent(
                            context,
                            CHAT_ID,
                            CONVERSATION_NOTIFICATION_ID
                        )
                    )
                        .addRemoteInput(
                            RemoteInput.Builder(ChatMessageReplyReceiver.KEY_TEXT_REPLY).build()
                        )
                        .build()
                )
                .addExtras(
                    Bundle().apply {
                        putLongArray(
                            ChatMessageNotificationManager.EXTRA_MESSAGE_IDS,
                            messages.map { it.second }.toLongArray()
                        )
                    }
                )
                .build()
        return StatusBarNotification(
            context.packageName,
            context.packageName,
            CONVERSATION_NOTIFICATION_ID,
            null,
            0,
            0,
            0,
            notification,
            Process.myUserHandle(),
            0L,
        )
    }

    private inline fun withBase64HandleStubbed(block: () -> Unit) {
        mockStatic(MegaApiJava::class.java).use { mocked ->
            mocked.`when`<String> { MegaApiJava.userHandleToBase64(any()) }
                .thenAnswer { (it.arguments[0] as Long).toString() }
            block()
        }
    }

    private companion object {
        const val CHAT_ID = 123L
        const val MESSAGE_ID = 456L
        const val EARLIER_MESSAGE_ID = 455L
        const val SENDER_HANDLE = 789L
        const val CHAT_TITLE = "Chat title"
        const val SENDER_NAME = "Sender"
        const val INCOMING_TEXT = "Hello"
        const val EARLIER_TEXT = "Earlier message"
        const val REPLY_TEXT = "Reply from notification"
        val CONVERSATION_NOTIFICATION_ID = "ChatMessages$CHAT_ID".hashCode()
    }
}
