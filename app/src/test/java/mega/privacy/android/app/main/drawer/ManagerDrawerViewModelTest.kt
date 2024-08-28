package mega.privacy.android.app.main.drawer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.GetBackupsNode
import mega.privacy.android.app.main.drawer.ManagerDrawerViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.usecase.HasBackupsChildren
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.GetCurrentUserStatusUseCase
import mega.privacy.android.domain.usecase.contact.MonitorMyChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.notifications.GetEnabledNotificationsUseCase
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ManagerDrawerViewModelTest {
    private lateinit var underTest: ManagerDrawerViewModel
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val getCurrentUserChatStatusUseCase: GetCurrentUserStatusUseCase = mock {
        onBlocking { invoke() }.thenReturn(UserChatStatus.Invalid)
    }
    private val hasBackupsChildren: HasBackupsChildren = mock()
    private val getBackupsNode: GetBackupsNode = mock()
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase = mock()
    private val monitorMyChatOnlineStatusUseCase: MonitorMyChatOnlineStatusUseCase = mock()
    private val rootNodeExistsUseCase: RootNodeExistsUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock {
        onBlocking { invoke(any()) }.thenReturn(false)
    }
    private val monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase = mock()
    private val monitorVerificationStatus: MonitorVerificationStatus = mock()
    private val getEnabledNotificationsUseCase: GetEnabledNotificationsUseCase = mock()

    @BeforeEach
    fun resetMocks() {
        reset(
            isConnectedToInternetUseCase,
            hasBackupsChildren,
            getBackupsNode,
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
        monitorStorageStateEventUseCase.stub {
            on { invoke() }.thenReturn(
                MutableStateFlow(
                    StorageStateEvent(
                        1L,
                        "",
                        0L,
                        "",
                        EventType.Storage,
                        StorageState.Change,
                    )
                )
            )
        }
        getCurrentUserChatStatusUseCase.stub {
            onBlocking { invoke() }.thenReturn(UserChatStatus.Invalid)
        }
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(false)
        }
    }

    private fun initTestClass() {
        underTest = ManagerDrawerViewModel(
            isConnectedToInternetUseCase,
            monitorStorageStateEventUseCase,
            getCurrentUserChatStatusUseCase,
            hasBackupsChildren,
            getBackupsNode,
            monitorNodeUpdatesUseCase,
            monitorMyChatOnlineStatusUseCase,
            monitorVerificationStatus,
            rootNodeExistsUseCase,
            monitorConnectivityUseCase,
            getFeatureFlagValueUseCase,
            getEnabledNotificationsUseCase,
            monitorMyAccountUpdateUseCase,
        )
    }

    @ParameterizedTest(name = "with isRootNodeExist {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isRootNodeExist update correctly when call checkRootNode`(exist: Boolean) =
        runTest {
            whenever(rootNodeExistsUseCase()).thenReturn(exist)

            initTestClass()

            underTest.state.test {
                assertThat(awaitItem().isRootNodeExist).isEqualTo(exist)
            }
        }

    @ParameterizedTest(name = "with hasBackupsChildren {0}")
    @ValueSource(booleans = [true, false])
    fun `test that hasBackupsChildren update correctly when call checkBackupChildren`(hasChild: Boolean) =
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
    fun `test that userStatus update correctly when call monitorCurrentUserStatus emit`(status: UserChatStatus) =
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
    fun `test that backUpNodeHandle update correctly when call loadBackupNode`() =
        runTest {
            val node = mock<MegaNode> {
                on { handle }.thenReturn(1L)
            }
            whenever(getBackupsNode()).thenReturn(node)

            initTestClass()

            underTest.state.test {
                assertThat(awaitItem().backUpNodeHandle).isEqualTo(node.handle)
            }
        }

    @ParameterizedTest(name = "test that when size of list of enabled notifications is {0} and PromoNotifications feature flag is set to {1} then showPromoTag is updated to {2}")
    @MethodSource("provideShowPromoTagParameters")
    fun `test that showPromoTag is updated when there is a change in the number of available promo notifications`(
        promoNotificationsCount: List<Int>,
        featureFlagValue: Boolean,
        expectedShowPromoTag: Boolean,
    ) = runTest {
        whenever(getEnabledNotificationsUseCase()).thenReturn(promoNotificationsCount)
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(featureFlagValue)

        initTestClass()

        underTest.state.test {
            assertThat(awaitItem().showPromoTag).isEqualTo(expectedShowPromoTag)
        }
    }

    private fun provideShowPromoTagParameters() = listOf(
        Arguments.of(emptyList<Int>(), false, false),
        Arguments.of(emptyList<Int>(), true, false),
        Arguments.of(listOf(1, 2), false, false),
        Arguments.of(listOf(1, 2, 3), true, true),
    )
}
