package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class GetScheduleMeetingDataUseCaseTest {

    private lateinit var underTest: GetScheduleMeetingDataUseCase

    private val getScheduledMeetingByChat = mock<GetScheduledMeetingByChat>()
    private val getNextSchedMeetingOccurrence = mock<GetNextSchedMeetingOccurrenceUseCase>()
    private val getChatRoom = mock<GetChatRoom>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        underTest = GetScheduleMeetingDataUseCase(
            getScheduledMeetingByChat,
            getNextSchedMeetingOccurrence,
            getChatRoom,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that getMeetingScheduleData return null`() =
        runTest {
            val chatId = 123L
            whenever(getScheduledMeetingByChat(chatId)).thenReturn(null)

            val result = underTest.invoke(chatId) { _, _ -> "" }

            assertThat(result).isNull()
        }

    @Test
    fun `test that getChatRoom is called accordingly`() =
        runTest {
            val chatId = 123L
            val chat = mock<ChatRoom> {
                on { chatId }.thenReturn(chatId)
            }
            whenever(getChatRoom(chatId)).thenReturn(chat)
            whenever(getScheduledMeetingByChat(chatId)).thenReturn(
                listOf(
                    ChatScheduledMeeting(
                        chatId = chatId,
                        parentSchedId = -1L,
                        isCanceled = false
                    )
                )
            )

            underTest.invoke(chatId) { _, _ -> "" }

            verify(getChatRoom).invoke(chatId)
        }

    @Test
    fun `test that getChatRoom return error accordingly`() =
        runTest {
            val chatId = 123L
            whenever(getChatRoom(chatId)).thenReturn(null)
            whenever(getScheduledMeetingByChat(chatId)).thenReturn(
                listOf(
                    ChatScheduledMeeting(
                        chatId = chatId,
                        parentSchedId = -1L,
                        isCanceled = false
                    )
                )
            )

            assertThrows<IllegalStateException> {
                underTest.invoke(chatId) { _, _ -> "" }
            }
        }
}
