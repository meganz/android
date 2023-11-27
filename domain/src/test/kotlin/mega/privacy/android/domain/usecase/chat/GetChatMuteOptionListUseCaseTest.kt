package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.repository.TimeSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatMuteOptionListUseCaseTest {

    private lateinit var underTest: GetChatMuteOptionListUseCase

    private val timeSystemRepository = mock<TimeSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetChatMuteOptionListUseCase(
            timeSystemRepository = timeSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(timeSystemRepository)
    }

    @Test
    fun `test that MuteUntilTurnBackOn is included in result when the chat list is not empty`() {
        val chatList = listOf(1L)
        assertThat(underTest(chatList)).isEqualTo(
            commonResults + ChatPushNotificationMuteOption.MuteUntilTurnBackOn
        )
    }

    @Test
    fun `test that until this morning is included in result when there is empty chat list and time is before 8am`() {
        whenever(timeSystemRepository.getCurrentHourOfDay()).thenReturn(7)
        whenever(timeSystemRepository.getCurrentMinute()).thenReturn(0)

        assertThat(underTest(emptyList())).isEqualTo(
            commonResults + ChatPushNotificationMuteOption.MuteUntilThisMorning

        )
    }

    @Test
    fun `test that until this morning is included in result when there is empty chat list and time is at 8am sharp`() {
        whenever(timeSystemRepository.getCurrentHourOfDay()).thenReturn(8)
        whenever(timeSystemRepository.getCurrentMinute()).thenReturn(0)

        assertThat(underTest(emptyList())).isEqualTo(
            commonResults + ChatPushNotificationMuteOption.MuteUntilThisMorning
        )
    }

    @Test
    fun `test that until this morning is included in result when there is empty chat list and time is after 8am`() {
        whenever(timeSystemRepository.getCurrentHourOfDay()).thenReturn(9)
        whenever(timeSystemRepository.getCurrentMinute()).thenReturn(0)

        assertThat(underTest(emptyList())).isEqualTo(
            commonResults + ChatPushNotificationMuteOption.MuteUntilTomorrowMorning
        )
    }

    private val commonResults = listOf(
        ChatPushNotificationMuteOption.Mute30Minutes,
        ChatPushNotificationMuteOption.Mute1Hour,
        ChatPushNotificationMuteOption.Mute6Hours,
        ChatPushNotificationMuteOption.Mute24Hours,
    )
}