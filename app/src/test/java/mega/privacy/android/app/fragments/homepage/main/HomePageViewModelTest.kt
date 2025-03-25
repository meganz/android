package mega.privacy.android.app.fragments.homepage.main

import androidx.work.impl.utils.forAll
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.contact.MonitorMyChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.MonitorLogoutUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.notifications.MonitorHomeBadgeCountUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HomePageViewModelTest {
    private val repository: HomepageRepository = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val monitorLogoutUseCase: MonitorLogoutUseCase = mock()
    private val monitorHomeBadgeCountUseCase: MonitorHomeBadgeCountUseCase = mock()
    private val monitorMyChatOnlineStatusUseCase: MonitorMyChatOnlineStatusUseCase = mock()
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase = mock()

    private lateinit var underTest: HomePageViewModel

    @BeforeAll
    fun setUp() {
        reset()
        initViewModel()
    }

    private fun initViewModel() {
        underTest = HomePageViewModel(
            repository,
            monitorConnectivityUseCase,
            monitorFetchNodesFinishUseCase,
            monitorLogoutUseCase,
            monitorHomeBadgeCountUseCase,
            monitorMyChatOnlineStatusUseCase,
        )
    }

    @BeforeEach
    fun reset() {
        whenever(monitorMyChatOnlineStatusUseCase()).thenReturn(emptyFlow())
        whenever(monitorHomeBadgeCountUseCase()).thenReturn(emptyFlow())
        whenever(monitorLogoutUseCase()).thenReturn(emptyFlow())
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(monitorFetchNodesFinishUseCase()).thenReturn(emptyFlow())
        reset(
            repository,
        )
    }

    @ParameterizedTest(name = "with monitorCurrentUserStatus emit {0}")
    @EnumSource(UserChatStatus::class)
    fun `test that userStatus update correctly when call monitorCurrentUserStatus emit`(status: UserChatStatus) =
        runTest {
            whenever(monitorMyChatOnlineStatusUseCase()).thenReturn(
                flow {
                    emit(OnlineStatus(1L, status, true))
                }
            )

            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().userChatStatus).isEqualTo(status)
            }
        }

    @Test
    fun `test that isConnected is updated correctly when monitorConnectivityUseCase emit connectivity status`() =
        runTest {
            val monitorConnectivityFakeFlow = MutableSharedFlow<Boolean>()
            whenever(monitorConnectivityUseCase()).thenReturn(monitorConnectivityFakeFlow)

            initViewModel()

            monitorConnectivityFakeFlow.emit(false)

            underTest.isConnected.test {
                assertThat(awaitItem()).isFalse()
            }

            monitorConnectivityFakeFlow.emit(true)

            underTest.isConnected.test {
                assertThat(awaitItem()).isTrue()
            }
        }
}