package mega.privacy.android.app.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.domain.entity.pushes.PushMessage
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ScheduledMeetingPushMessageNotificationManagerTest {

    private lateinit var underTest: ScheduledMeetingPushMessageNotificationManager

    private lateinit var context: Context
    private val notificationManagerCompat: NotificationManagerCompat = mock()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        underTest = ScheduledMeetingPushMessageNotificationManager(
            notificationManagerCompat,
        )
    }

    @Test
    fun `test that generated notification Id is the expected one`() = runTest {
        val pushMessage = PushMessage.ScheduledMeetingPushMessage(
            schedId = -1L,
            userHandle = -1L,
            chatRoomHandle = -1L,
            title = "Test title",
            description = "Test description",
            startTimestamp = 0,
            endTimestamp = 0,
            timezone = null,
            isStartReminder = false
        )

        underTest.show(context, pushMessage)

        verify(notificationManagerCompat).notify(eq(pushMessage.chatRoomHandle.toInt()), any())
    }

    @Test
    fun `test that generated notification Title is the expected one`() = runTest {
        val pushMessage = PushMessage.ScheduledMeetingPushMessage(
            schedId = -1L,
            userHandle = -1L,
            chatRoomHandle = -1L,
            title = "Test title",
            description = "Test description",
            startTimestamp = 0,
            endTimestamp = 0,
            timezone = null,
            isStartReminder = false
        )

        underTest.show(context, pushMessage)

        verify(notificationManagerCompat).notify(any(), argThat { arg: Notification ->
            arg.extras.getString("android.title").equals(pushMessage.title)
        })
    }

    @Test
    fun `test that show sets a content intent that opens the chat through MegaActivity`() =
        runTest {
            val pushMessage = PushMessage.ScheduledMeetingPushMessage(
                schedId = -1L,
                userHandle = -1L,
                chatRoomHandle = 123L,
                title = "Test title",
                description = "Test description",
                startTimestamp = 0,
                endTimestamp = 0,
                timezone = null,
                isStartReminder = false
            )
            val expected = MegaActivity.getIntentWithExtraDestinations(
                context,
                listOf(ShowChatMessagesNavKey(chatId = pushMessage.chatRoomHandle)),
            )

            underTest.show(context, pushMessage)

            val notification = argumentCaptor<Notification>().apply {
                verify(notificationManagerCompat).notify(any(), capture())
            }.firstValue
            val shadowPendingIntent = shadowOf(notification.contentIntent)
            val actual = shadowPendingIntent.savedIntent
            assertThat(actual.filterEquals(expected)).isTrue()
            val expectedExtras = requireNotNull(expected.extras)
            val actualExtras = requireNotNull(actual.extras)
            assertThat(actualExtras.keySet()).isEqualTo(expectedExtras.keySet())
            expectedExtras.keySet().forEach { key ->
                @Suppress("DEPRECATION")
                assertThat(actualExtras.get(key)).isEqualTo(expectedExtras.get(key))
            }
            assertThat(shadowPendingIntent.requestCode)
                .isEqualTo(pushMessage.chatRoomHandle.toInt())
            assertThat(shadowPendingIntent.flags).isEqualTo(
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT or
                        PendingIntent.FLAG_IMMUTABLE
            )
        }
}
