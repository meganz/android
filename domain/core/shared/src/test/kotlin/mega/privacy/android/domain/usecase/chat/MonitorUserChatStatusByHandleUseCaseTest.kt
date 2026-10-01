package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.usecase.contact.MonitorChatOnlineStatusUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorUserChatStatusByHandleUseCaseTest {

    private lateinit var underTest: MonitorUserChatStatusByHandleUseCase

    private val monitorChatOnlineStatusUseCase = mock<MonitorChatOnlineStatusUseCase>()

    private val userHandle = 123L

    @BeforeEach
    fun setup() {
        underTest = MonitorUserChatStatusByHandleUseCase(monitorChatOnlineStatusUseCase)
    }

    @AfterEach
    fun resetMocks() {
        reset(monitorChatOnlineStatusUseCase)
    }

    @ParameterizedTest(name = " if monitor chat online status returns {0}")
    @MethodSource("provideTestParameters")
    fun `test that monitor user chat status by handle returns correctly`(
        onlineStatusChanges: Flow<OnlineStatus>,
        expectedChanges: Flow<UserChatStatus>?,
    ) =
        runTest {
            whenever(monitorChatOnlineStatusUseCase()).thenReturn(onlineStatusChanges)
            underTest(userHandle).test {
                if (onlineStatusChanges == emptyFlow<OnlineStatus>() || expectedChanges == null) {
                    awaitComplete()
                } else {
                    expectedChanges.collect {
                        Truth.assertThat(awaitItem()).isEqualTo(it)
                    }
                    awaitComplete()
                }
            }
        }

    private fun provideTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(emptyFlow<OnlineStatus>(), null),
        Arguments.of(
            flowOf(OnlineStatus(1L, UserChatStatus.Online, false)),
            null
        ),
        Arguments.of(
            flowOf(OnlineStatus(userHandle, UserChatStatus.Online, false)),
            flowOf(UserChatStatus.Online)
        ),
        Arguments.of(
            flowOf(
                OnlineStatus(userHandle, UserChatStatus.Online, false),
                OnlineStatus(userHandle, UserChatStatus.Offline, false)
            ),
            flowOf(UserChatStatus.Online, UserChatStatus.Offline)
        ),
        Arguments.of(
            flowOf(
                OnlineStatus(1L, UserChatStatus.Online, false),
                OnlineStatus(userHandle, UserChatStatus.Offline, false)
            ),
            flowOf(UserChatStatus.Offline)
        ),
        Arguments.of(
            flowOf(
                OnlineStatus(userHandle, UserChatStatus.Online, false),
                OnlineStatus(1L, UserChatStatus.Online, false),
                OnlineStatus(userHandle, UserChatStatus.Offline, false)
            ),
            flowOf(UserChatStatus.Online, UserChatStatus.Offline)
        ),
        Arguments.of(
            flowOf(
                OnlineStatus(userHandle, UserChatStatus.Online, false),
                OnlineStatus(userHandle, UserChatStatus.Away, false),
                OnlineStatus(1L, UserChatStatus.Online, false),
                OnlineStatus(userHandle, UserChatStatus.Offline, false)
            ),
            flowOf(UserChatStatus.Online, UserChatStatus.Away, UserChatStatus.Offline)
        ),
    )
}