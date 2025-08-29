package mega.privacy.android.app.main.drawer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.HasBackupsChildren
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.chat.GetCurrentUserStatusUseCase
import mega.privacy.android.domain.usecase.contact.MonitorMyChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.backup.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.notifications.GetEnabledNotificationsUseCase
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

/**
 * Test class for [ManagerDrawerViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ManagerDrawerViewModelTest {
    private lateinit var underTest: ManagerDrawerViewModel
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase = mock()
    private val getCurrentUserChatStatusUseCase: GetCurrentUserStatusUseCase = mock {
        onBlocking { invoke() }.thenReturn(UserChatStatus.Invalid)
    }
    private val hasBackupsChildren: HasBackupsChildren = mock()
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase = mock()
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase = mock()
    private val monitorMyChatOnlineStatusUseCase: MonitorMyChatOnlineStatusUseCase = mock()
    private val rootNodeExistsUseCase: RootNodeExistsUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase = mock()
    private val monitorVerificationStatus: MonitorVerificationStatus = mock()
    private val getEnabledNotificationsUseCase: GetEnabledNotificationsUseCase = mock()
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase = mock()

    @BeforeEach
    fun resetMocks() {
        reset(
            isConnectedToInternetUseCase,
            hasBackupsChildren,
            getBackupsNodeUseCase,
            rootNodeExistsUseCase,
            monitorMyAccountUpdateUseCase,
            monitorVerificationStatus,
            getEnabledNotificationsUseCase,
        )
        monitorMyChatOnlineStatusUseCase.stub {
            on { invoke() }.thenReturn(emptyFlow())
        }
        monitorConnectivityUseCase.stub {
            on { invoke() }.thenReturn(emptyFlow())
        }
        monitorNodeUpdatesUseCase.stub {
            on { invoke() }.thenReturn(emptyFlow())
        }
        monitorStorageStateUseCase.stub {
            on { invoke() }.thenReturn(
                MutableStateFlow(
                    StorageState.Green
                )
            )
        }
        getCurrentUserChatStatusUseCase.stub {
            onBlocking { invoke() }.thenReturn(UserChatStatus.Invalid)
        }
    }

    private fun initTestClass() {
        underTest = ManagerDrawerViewModel(
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            getCurrentUserStatusUseCase = getCurrentUserChatStatusUseCase,
            hasBackupsChildren = hasBackupsChildren,
            getBackupsNodeUseCase = getBackupsNodeUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorMyChatOnlineStatusUseCase = monitorMyChatOnlineStatusUseCase,
            monitorVerificationStatus = monitorVerificationStatus,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getEnabledNotificationsUseCase = getEnabledNotificationsUseCase,
            monitorFetchNodesFinishUseCase = monitorFetchNodesFinishUseCase,
            monitorMyAccountUpdateUseCase = monitorMyAccountUpdateUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase
        )
    }

    @ParameterizedTest(name = "with isRootNodeExist {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isRootNodeExist updates correctly when calling checkRootNode`(exist: Boolean) =
        runTest {
            whenever(rootNodeExistsUseCase()).thenReturn(exist)

            initTestClass()

            underTest.state.test {
                assertThat(awaitItem().isRootNodeExist).isEqualTo(exist)
            }
        }

    @ParameterizedTest(name = "with hasBackupsChildren {0}")
    @ValueSource(booleans = [true, false])
    fun `test that hasBackupsChildren updates correctly when calling checkBackupChildren`(hasChild: Boolean) =
        runTest {
            whenever(hasBackupsChildren()).thenReturn(hasChild)

            initTestClass()

            underTest.state.test {
                assertThat(awaitItem().hasBackupsChildren).isEqualTo(hasChild)
            }
        }

    @ParameterizedTest(name = "with userChatStatus {0}")
    @EnumSource(UserChatStatus::class)
    fun `test that userStatus update correctly when call getCurrentUserStatus`(status: UserChatStatus) =
        runTest {
            whenever(getCurrentUserChatStatusUseCase()).thenReturn(status)

            initTestClass()

            underTest.state.test {
                assertThat(awaitItem().userChatStatus).isEqualTo(status)
            }
        }

    @ParameterizedTest(name = "with monitorCurrentUserStatus emit {0}")
    @EnumSource(UserChatStatus::class)
    fun `test that userStatus updates correctly when calling monitorCurrentUserStatus emit`(status: UserChatStatus) =
        runTest {
            whenever(monitorMyChatOnlineStatusUseCase()).thenReturn(
                flow {
                    emit(OnlineStatus(1L, status, true))
                }
            )

            initTestClass()

            underTest.state.test {
                assertThat(awaitItem().userChatStatus).isEqualTo(status)
            }
        }

    @Test
    fun `test that backupsNodeHandle updates correctly when calling loadBackupNode`() =
        runTest {
            val expectedNodeId = NodeId(1L)
            val backupsNode = mock<FileNode> {
                on { id }.thenReturn(expectedNodeId)
            }
            whenever(getBackupsNodeUseCase()).thenReturn(backupsNode)

            initTestClass()

            underTest.state.test {
                assertThat(awaitItem().backupsNodeHandle).isEqualTo(expectedNodeId)
            }
        }

    @ParameterizedTest(name = "with value {0}")
    @ValueSource(booleans = [true, false])
    fun `test that root node exists is updated correctly when fetch nodes finished is received`(
        isRootNodeExist: Boolean,
    ) =
        runTest {
            whenever(monitorFetchNodesFinishUseCase()).thenReturn(flowOf(true))
            whenever(rootNodeExistsUseCase()).thenReturn(isRootNodeExist)

            initTestClass()

            underTest.state.test {
                assertThat(awaitItem().isRootNodeExist).isEqualTo(isRootNodeExist)
            }
        }

    @ParameterizedTest(name = "test that when size of list of enabled notifications is {0} then showPromoTag is updated to {1}")
    @MethodSource("provideShowPromoTagParameters")
    fun `test that showPromoTag is updated when there is a change in the number of available promo notifications`(
        promoNotificationsCount: List<Int>,
        expectedShowPromoTag: Boolean,
    ) = runTest {
        whenever(getEnabledNotificationsUseCase()).thenReturn(promoNotificationsCount)

        initTestClass()

        underTest.state.test {
            assertThat(awaitItem().showPromoTag).isEqualTo(expectedShowPromoTag)
        }
    }

    private fun provideShowPromoTagParameters() = listOf(
        Arguments.of(emptyList<Int>(), false),
        Arguments.of(listOf(1, 2, 3), true),
    )

    @ParameterizedTest
    @EnumSource(StorageState::class)
    fun `test that the correct storage state is successfully set`(storageState: StorageState) =
        runTest {
            whenever(monitorStorageStateUseCase()).thenReturn(flowOf(storageState))

            initTestClass()

            underTest.state.test {
                assertThat(expectMostRecentItem().storageState).isEqualTo(storageState)
            }
        }
}
