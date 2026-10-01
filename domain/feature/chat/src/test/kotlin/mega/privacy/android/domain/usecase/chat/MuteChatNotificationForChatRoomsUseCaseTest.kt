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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.stream.Stream


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MuteChatNotificationForChatRoomsUseCaseTest {

    private lateinit var underTest: MuteChatNotificationForChatRoomsUseCase

    private val notificationsRepository: NotificationsRepository = mock()
    private val timeSystemRepository: TimeSystemRepository = mock()

    private val chatIdList = listOf(1L)

    @BeforeAll
    fun setup() {
        underTest = MuteChatNotificationForChatRoomsUseCase(
            notificationsRepository = notificationsRepository,
            timeSystemRepository = timeSystemRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(notificationsRepository, timeSystemRepository)
    }

    @Test
    fun `test that nothing happens when a empty chat list is passed`() = runTest {
        underTest(emptyList(), ChatPushNotificationMuteOption.Mute)
        verify(notificationsRepository, times(0)).setChatEnabled(emptyList(), false)
        verify(timeSystemRepository, times(0)).getCurrentTimeInMillis()
    }

    @Test
    fun `test that push notification is disabled if mute option is Mute`() = runTest {
        underTest(chatIdList, ChatPushNotificationMuteOption.Mute)
        verify(notificationsRepository).setChatEnabled(chatIdList, false)
        verifyNoInteractions(timeSystemRepository)
    }

    @Test
    fun `test that push notification is disabled if mute option is MuteUntilTurnBackOn`() =
        runTest {
            underTest(chatIdList, ChatPushNotificationMuteOption.Mute)
            verify(notificationsRepository).setChatEnabled(chatIdList, false)
            verifyNoInteractions(timeSystemRepository)
        }

    @Test
    fun `test that push notification is disabled if mute option is Unmute`() =
        runTest {
            underTest(chatIdList, ChatPushNotificationMuteOption.Unmute)
            verify(notificationsRepository).setChatEnabled(chatIdList, true)
            verifyNoInteractions(timeSystemRepository)
        }


    @ParameterizedTest(name = " at timestamp {1} when mute option is set to {0}")
    @MethodSource("provideMuteForPeriodParameters")
    fun `test that do-not-disturb time is set`(
        muteOption: ChatPushNotificationMuteOption,
        expectedTime: Long,
    ) = runTest {
        val timestampOfNow = 0L
        whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(timestampOfNow)

        underTest(chatIdList, muteOption)
        verify(notificationsRepository).setChatDoNotDisturb(chatIdList, expectedTime)
    }

    private fun provideMuteForPeriodParameters() =
        Stream.of(
            Arguments.of(
                ChatPushNotificationMuteOption.Mute30Minutes,
                TimeUnit.SECONDS.convert(30, TimeUnit.MINUTES)
            ),
            Arguments.of(
                ChatPushNotificationMuteOption.Mute1Hour,
                TimeUnit.SECONDS.convert(1, TimeUnit.HOURS)
            ),
            Arguments.of(
                ChatPushNotificationMuteOption.Mute6Hours,
                TimeUnit.SECONDS.convert(6, TimeUnit.HOURS)
            ),
            Arguments.of(
                ChatPushNotificationMuteOption.Mute24Hours,
                TimeUnit.SECONDS.convert(24, TimeUnit.HOURS)
            ),
        )

    @ParameterizedTest(name = " when mute option is {0}")
    @MethodSource("provideMuteUntilMorningParameters")
    fun `test that do-no-disturb time is set correctly`(
        muteOption: ChatPushNotificationMuteOption,
        expectedTime: Long,
    ) = runTest {
        whenever(timeSystemRepository.getCurrentTimeInMillis()).thenReturn(0L)
        underTest(chatIdList, muteOption)
        verify(notificationsRepository).setChatDoNotDisturb(
            chatIdList,
            expectedTime
        )
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
                timestampOfThisMorning
            ),
            Arguments.of(
                ChatPushNotificationMuteOption.MuteUntilTomorrowMorning,
                timestampOfThisMorning + TimeUnit.SECONDS.convert(1, TimeUnit.DAYS)
            ),
        )
    }

}