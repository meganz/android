package mega.privacy.android.app.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.pushes.PushMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

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
}
