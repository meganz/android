package mega.privacy.android.app.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.domain.entity.pushes.PushMessage
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ScheduledMeetingPushMessageNotificationTest {

    private lateinit var underTest: ScheduledMeetingPushMessageNotification

    private lateinit var context: Context
    private val notificationManagerCompat: NotificationManagerCompat = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock {
        onBlocking { invoke(AppFeatures.NewChatActivity) }.thenReturn(false)
    }

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        underTest = ScheduledMeetingPushMessageNotification(
            notificationManagerCompat,
            getFeatureFlagValueUseCase
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

    @Test(expected = IllegalStateException::class)
    fun `test that CallPushMessage causes an exception`() = runTest {
        val pushMessage = PushMessage.CallPushMessage

        underTest.show(context, pushMessage)
    }
}
