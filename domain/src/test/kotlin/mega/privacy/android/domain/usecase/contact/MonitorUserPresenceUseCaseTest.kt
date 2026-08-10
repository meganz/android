package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.contacts.UserPresence
import mega.privacy.android.domain.usecase.chat.MonitorUserChatStatusByHandleUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorUserPresenceUseCaseTest {
    private lateinit var underTest: MonitorUserPresenceUseCase

    private val statusFlow = MutableSharedFlow<UserChatStatus>()
    private val lastGreenFlow = MutableSharedFlow<Int>()
    private val getUserOnlineStatusByHandleUseCase = mock<GetUserOnlineStatusByHandleUseCase>()
    private val monitorUserChatStatusByHandleUseCase = mock<MonitorUserChatStatusByHandleUseCase>()
    private val monitorUserLastGreenUpdatesUseCase = mock<MonitorUserLastGreenUpdatesUseCase>()
    private val requestUserLastGreenUseCase = mock<RequestUserLastGreenUseCase>()

    private val userHandle = 123L

    @BeforeAll
    fun setUp() {
        underTest = MonitorUserPresenceUseCase(
            getUserOnlineStatusByHandleUseCase = getUserOnlineStatusByHandleUseCase,
            monitorUserChatStatusByHandleUseCase = monitorUserChatStatusByHandleUseCase,
            monitorUserLastGreenUpdatesUseCase = monitorUserLastGreenUpdatesUseCase,
            requestUserLastGreenUseCase = requestUserLastGreenUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getUserOnlineStatusByHandleUseCase,
            monitorUserChatStatusByHandleUseCase,
            monitorUserLastGreenUpdatesUseCase,
            requestUserLastGreenUseCase,
        )
        whenever(monitorUserChatStatusByHandleUseCase(userHandle)).thenReturn(statusFlow)
        whenever(monitorUserLastGreenUpdatesUseCase(userHandle)).thenReturn(lastGreenFlow)
    }

    @Test
    fun `test that invoke emits the initial status from the getter with unknown last green`() =
        runTest {
            whenever(getUserOnlineStatusByHandleUseCase(userHandle))
                .thenReturn(UserChatStatus.Online)

            underTest(userHandle).test {
                assertThat(awaitItem()).isEqualTo(
                    UserPresence(status = UserChatStatus.Online, lastGreenMinutes = null)
                )
            }
        }

    @Test
    fun `test that invoke emits the updated status when a status update is received`() = runTest {
        whenever(getUserOnlineStatusByHandleUseCase(userHandle)).thenReturn(UserChatStatus.Online)

        underTest(userHandle).test {
            assertThat(awaitItem().status).isEqualTo(UserChatStatus.Online)
            statusFlow.emit(UserChatStatus.Busy)
            assertThat(awaitItem().status).isEqualTo(UserChatStatus.Busy)
        }
    }

    @Test
    fun `test that invoke emits the last green when a last green update is received`() = runTest {
        whenever(getUserOnlineStatusByHandleUseCase(userHandle)).thenReturn(UserChatStatus.Away)

        underTest(userHandle).test {
            assertThat(awaitItem().lastGreenMinutes).isNull()
            lastGreenFlow.emit(15)
            val presence = awaitItem()
            assertThat(presence.lastGreenMinutes).isEqualTo(15)
            assertThat(presence.status).isEqualTo(UserChatStatus.Away)
        }
    }

    @Test
    fun `test that invoke requests last green when the initial status is away`() = runTest {
        whenever(getUserOnlineStatusByHandleUseCase(userHandle)).thenReturn(UserChatStatus.Away)

        underTest(userHandle).test {
            awaitItem()
            verify(requestUserLastGreenUseCase)(userHandle)
        }
    }

    @Test
    fun `test that invoke requests last green when a status update is offline`() = runTest {
        whenever(getUserOnlineStatusByHandleUseCase(userHandle)).thenReturn(UserChatStatus.Online)

        underTest(userHandle).test {
            awaitItem()
            verify(requestUserLastGreenUseCase, never())(userHandle)
            statusFlow.emit(UserChatStatus.Offline)
            awaitItem()
            verify(requestUserLastGreenUseCase)(userHandle)
        }
    }

    @Test
    fun `test that invoke does not request last green when the status is online`() = runTest {
        whenever(getUserOnlineStatusByHandleUseCase(userHandle)).thenReturn(UserChatStatus.Online)

        underTest(userHandle).test {
            awaitItem()
            statusFlow.emit(UserChatStatus.Busy)
            awaitItem()
            verify(requestUserLastGreenUseCase, never())(userHandle)
        }
    }
}
