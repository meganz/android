package mega.privacy.android.app.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ChatMessageReplyReceiverTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test that getPendingIntent targets the reply receiver with the chat and notification ids`() {
        val pendingIntent = ChatMessageReplyReceiver.getPendingIntent(
            context = context,
            chatId = CHAT_ID,
            notificationId = NOTIFICATION_ID,
        )

        val intent = shadowOf(pendingIntent).savedIntent
        assertThat(intent.component?.className)
            .isEqualTo(ChatMessageReplyReceiver::class.java.name)
        assertThat(intent.getLongExtra("EXTRA_CHAT_ID", -1L)).isEqualTo(CHAT_ID)
        assertThat(intent.getIntExtra("EXTRA_NOTIFICATION_ID", 0)).isEqualTo(NOTIFICATION_ID)
        assertThat(shadowOf(pendingIntent).requestCode).isEqualTo(NOTIFICATION_ID)
    }

    @Test
    fun `test that getPendingIntent builds intents for different chats that do not filter equal`() {
        val first = ChatMessageReplyReceiver.getPendingIntent(context, CHAT_ID, NOTIFICATION_ID)
        val second =
            ChatMessageReplyReceiver.getPendingIntent(context, OTHER_CHAT_ID, NOTIFICATION_ID)

        val firstIntent = shadowOf(first).savedIntent
        val secondIntent = shadowOf(second).savedIntent
        assertThat(firstIntent.filterEquals(secondIntent)).isFalse()
        assertThat(secondIntent.getLongExtra("EXTRA_CHAT_ID", -1L)).isEqualTo(OTHER_CHAT_ID)
    }

    @Test
    fun `test that getPendingIntent builds intents for different messages of one chat that do not filter equal`() {
        val first = ChatMessageReplyReceiver.getPendingIntent(context, CHAT_ID, NOTIFICATION_ID)
        val second =
            ChatMessageReplyReceiver.getPendingIntent(context, CHAT_ID, OTHER_NOTIFICATION_ID)

        assertThat(shadowOf(first).savedIntent.filterEquals(shadowOf(second).savedIntent))
            .isFalse()
    }

    private companion object {
        const val CHAT_ID = 123L
        const val OTHER_CHAT_ID = 789L
        const val NOTIFICATION_ID = 456
        const val OTHER_NOTIFICATION_ID = 457
    }
}
