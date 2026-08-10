package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MuteChatPushNotificationUseCaseTest {

    private lateinit var underTest: MuteChatPushNotificationUseCase

    private val notificationsRepository: NotificationsRepository = mock()
    private val timeSystemRepository: TimeSystemRepository = mock()

    private val chatIds = listOf(1L, 2L)

    @BeforeAll
    fun setup() {
        underTest = MuteChatPushNotificationUseCase(
            notificationsRepository = notificationsRepository,
            timeSystemRepository = timeSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(notificationsRepository, timeSystemRepository)
    }

    @Test
    fun `test that setChatEnabled is called with false when option is Mute`() = runTest {
        underTest(chatIds, ChatPushNotificationMuteOption.Mute)
        verify(notificationsRepository).setChatEnabled(chatIds, false)
        verifyNoInteractions(timeSystemRepository)
    }

    @Test
    fun `test that setChatEnabled is called with false when option is MuteUntilTurnBackOn`() =
        runTest {
            underTest(chatIds, ChatPushNotificationMuteOption.MuteUntilTurnBackOn)
            verify(notificationsRepository).setChatEnabled(chatIds, false)
            verifyNoInteractions(timeSystemRepository)
        }

    @Test
    fun `test that setChatEnabled is called with true when option is Unmute`() = runTest {
        underTest(chatIds, ChatPushNotificationMuteOption.Unmute)
        verify(notificationsRepository).setChatEnabled(chatIds, true)
        verifyNoInteractions(timeSystemRepository)
    }

    @Test
    fun `test that setChatsEnabled is called with false when chatIds is null and option is Mute`() =
        runTest {
            underTest(null, ChatPushNotificationMuteOption.Mute)
            verify(notificationsRepository).setChatsEnabled(false)
            verifyNoInteractions(timeSystemRepository)
        }

    @Test
    fun `test that setChatsEnabled is called with true when chatIds is null and option is Unmute`() =
        runTest {
            underTest(null, ChatPushNotificationMuteOption.Unmute)
            verify(notificationsRepository).setChatsEnabled(true)
            verifyNoInteractions(timeSystemRepository)
        }

    @Test
    fun `test that setChatsEnabled is called when chatIds is empty and option is Mute`() =
        runTest {
            underTest(emptyList(), ChatPushNotificationMuteOption.Mute)
            verify(notificationsRepository).setChatsEnabled(false)
            verifyNoInteractions(timeSystemRepository)
        }

    @ParameterizedTest(name = "test that setChatDoNotDisturb is called at {1} when option is {0}")
    @MethodSource("provideMuteForPeriodParameters")
    fun `test that setChatDoNotDisturb is called with the right timestamp for timed options`(
        muteOption: ChatPushNotificationMuteOption,
        expectedTime: Long,
    ) = runTest {
        whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(0L)

        underTest(chatIds, muteOption)
        verify(notificationsRepository).setChatDoNotDisturb(chatIds, expectedTime)
    }

    @ParameterizedTest(name = "test that setChatsDoNotDisturb is called at {1} when option is {0}")
    @MethodSource("provideMuteForPeriodParameters")
    fun `test that setChatsDoNotDisturb is called when chatIds is null for timed options`(
        muteOption: ChatPushNotificationMuteOption,
        expectedTime: Long,
    ) = runTest {
        whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(0L)

        underTest(null, muteOption)
        verify(notificationsRepository).setChatsDoNotDisturb(expectedTime)
    }

    private fun provideMuteForPeriodParameters() = Stream.of(
        Arguments.of(
            ChatPushNotificationMuteOption.Mute30Minutes,
            TimeUnit.SECONDS.convert(30, TimeUnit.MINUTES),
        ),
        Arguments.of(
            ChatPushNotificationMuteOption.Mute1Hour,
            TimeUnit.SECONDS.convert(1, TimeUnit.HOURS),
        ),
        Arguments.of(
            ChatPushNotificationMuteOption.Mute6Hours,
            TimeUnit.SECONDS.convert(6, TimeUnit.HOURS),
        ),
        Arguments.of(
            ChatPushNotificationMuteOption.Mute24Hours,
            TimeUnit.SECONDS.convert(24, TimeUnit.HOURS),
        ),
    )

    @ParameterizedTest(name = "test that morning timestamp is set when option is {0}")
    @MethodSource("provideMuteUntilMorningParameters")
    fun `test that setChatDoNotDisturb is called with morning timestamp for morning options`(
        muteOption: ChatPushNotificationMuteOption,
        expectedTime: Long,
    ) = runTest {
        whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(0L)
        underTest(chatIds, muteOption)
        verify(notificationsRepository).setChatDoNotDisturb(chatIds, expectedTime)
    }

    @ParameterizedTest(name = "test that morning timestamp is set globally when option is {0}")
    @MethodSource("provideMuteUntilMorningParameters")
    fun `test that setChatsDoNotDisturb is called with morning timestamp when chatIds is null`(
        muteOption: ChatPushNotificationMuteOption,
        expectedTime: Long,
    ) = runTest {
        whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(0L)
        underTest(null, muteOption)
        verify(notificationsRepository).setChatsDoNotDisturb(expectedTime)
    }

    private fun provideMuteUntilMorningParameters(): Stream<Arguments> {
        val timestampOfThisMorning = Calendar.getInstance().apply {
            timeInMillis = 0L
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000

        return Stream.of(
            Arguments.of(
                ChatPushNotificationMuteOption.MuteUntilThisMorning,
                timestampOfThisMorning,
            ),
            Arguments.of(
                ChatPushNotificationMuteOption.MuteUntilTomorrowMorning,
                timestampOfThisMorning + TimeUnit.SECONDS.convert(1, TimeUnit.DAYS),
            ),
        )
    }
}
