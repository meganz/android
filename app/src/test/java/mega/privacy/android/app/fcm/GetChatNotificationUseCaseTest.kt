package mega.privacy.android.app.fcm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.pushes.PushMessage
import mega.privacy.android.domain.usecase.GetChatRoom
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GetChatNotificationUseCaseTest {

    private lateinit var underTest: GetChatNotificationUseCase

    private val getChatRoomUseCase: GetChatRoom = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val context: Context = ApplicationProvider.getApplicationContext()
        underTest = GetChatNotificationUseCase(
            context,
            getChatRoomUseCase,
            testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that ScheduledMeetingPushMessage notification is generated as expected`() = runTest {
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
        whenever(getChatRoomUseCase.invoke(any())).thenReturn(null)

        val result = underTest.invoke(pushMessage)
        val notificationId = result.first
        val notification = result.second

        Truth.assertThat(notificationId).isEqualTo(pushMessage.chatRoomHandle.toInt())
        Truth.assertThat(notification.extras.getString("android.title"))
            .isEqualTo(pushMessage.title)
    }

    @Test
    fun `test that ChatRoom updated title is shown as expected`() = runTest {
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
        val chatRoom = ChatRoom(
            chatId = pushMessage.chatRoomHandle,
            title = "Updated Test title"
        )
        whenever(getChatRoomUseCase.invoke(any())).thenReturn(chatRoom)

        val result = underTest.invoke(pushMessage).second

        Truth.assertThat(result.extras.getString("android.title")).isEqualTo(chatRoom.title)
    }

    @Test(expected = IllegalStateException::class)
    fun `test that CallPushMessage causes an exception`() = runTest {
        val pushMessage = PushMessage.CallPushMessage

        underTest.invoke(pushMessage)
    }
}
