package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetScheduledMeetingByChatUseCaseTest {

    private lateinit var underTest: GetScheduledMeetingByChatUseCase

    private val callRepository: CallRepository = mock()

    private val chatId = 1L

    @BeforeEach
    fun setUp() {
        underTest = GetScheduledMeetingByChatUseCase(
            callRepository = callRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(callRepository)
    }

    @ParameterizedTest
    @MethodSource("provideChatScheduledMeetings")
    fun `test that the correct list of scheduled meetings is returned`(
        scheduledMeetings: List<ChatScheduledMeeting>?,
    ) = runTest {
        whenever(callRepository.getScheduledMeetingsByChat(chatId)) doReturn scheduledMeetings

        val actual = underTest(chatId = chatId)

        assertThat(actual).isEqualTo(scheduledMeetings)
    }

    private fun provideChatScheduledMeetings() = Stream.of(
        Arguments.of(null),
        Arguments.of(
            listOf(
                ChatScheduledMeeting(chatId = chatId),
                ChatScheduledMeeting(chatId = chatId)
            )
        )
    )
}
