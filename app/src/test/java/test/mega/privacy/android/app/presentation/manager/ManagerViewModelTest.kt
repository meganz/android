package test.mega.privacy.android.app.presentation.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.app.domain.usecase.GetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.GetSecondarySyncHandle
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.entity.verification.UnVerified
import mega.privacy.android.domain.entity.verification.VerificationStatus
import mega.privacy.android.domain.entity.verification.Verified
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.usecase.CheckCameraUpload
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlerts
import mega.privacy.android.domain.usecase.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.GetUnverifiedOutgoingShares
import mega.privacy.android.domain.usecase.HasInboxChildren
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.SendStatisticsMediaDiscovery
import mega.privacy.android.domain.usecase.account.Check2FADialog
import mega.privacy.android.domain.usecase.account.SetLatestTargetPath
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import nz.mega.sdk.MegaUserAlert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ManagerViewModelTest {
    private lateinit var underTest: ManagerViewModel

    private val monitorGlobalUpdates = MutableStateFlow<GlobalUpdate>(GlobalUpdate.OnReloadNeeded)
    private val monitorNodeUpdates = MutableSharedFlow<NodeUpdate>()
    private val monitorContactUpdates = MutableSharedFlow<UserUpdate>()
    private val getNumUnreadUserAlerts =
        mock<GetNumUnreadUserAlerts> { onBlocking { invoke() }.thenReturn(0) }
    private val hasInboxChildren =
        mock<HasInboxChildren> { onBlocking { invoke() }.thenReturn(false) }
    private val monitorContactRequestUpdates = MutableStateFlow(emptyList<ContactRequest>())

    private val initialIsFirsLoginValue = true
    private val sendStatisticsMediaDiscovery = mock<SendStatisticsMediaDiscovery>()
    private val savedStateHandle = SavedStateHandle(
        mapOf(
            ManagerViewModel.isFirstLoginKey to initialIsFirsLoginValue
        )
    )
    private val monitorMyAvatarFile = mock<MonitorMyAvatarFile> {
        onBlocking { invoke() }.thenReturn(
            flow { awaitCancellation() })
    }
    private val getInboxNode = mock<GetInboxNode> { onBlocking { invoke() }.thenReturn(null) }
    private val monitorStorageState = mock<MonitorStorageStateEvent> {
        onBlocking { invoke() }.thenReturn(
            MutableStateFlow(
                StorageStateEvent(
                    0L,
                    "",
                    0L,
                    "",
                    EventType.Storage,
                    StorageState.Unknown
                )
            )
        )
    }
    private val monitorViewType = mock<MonitorViewType> {
        onBlocking { invoke() }.thenReturn(
            flow { awaitCancellation() })
    }
    private val getPrimarySyncHandle = mock<GetPrimarySyncHandle>()
    private val getSecondarySyncHandle = mock<GetSecondarySyncHandle>()
    private val checkCameraUpload = mock<CheckCameraUpload>()
    private val getCloudSortOrder =
        mock<GetCloudSortOrder> { onBlocking { invoke() }.thenReturn(SortOrder.ORDER_ALPHABETICAL_ASC) }
    private val monitorConnectivity = mock<MonitorConnectivity>()
    private val getFeatureFlagValue =
        mock<GetFeatureFlagValue> { onBlocking { invoke(any()) }.thenReturn(false) }
    private val shareDataList = listOf(
        ShareData("user", 8766L, 0, 987654678L, true, false),
        ShareData("user", 8766L, 0, 987654678L, true, false)
    )
    private val getUnverifiedOutgoingShares = mock<GetUnverifiedOutgoingShares> {
        onBlocking {
            invoke(
                any()
            )
        }.thenReturn(
            shareDataList
        )
    }
    private val getUnverifiedIncomingShares = mock<GetUnverifiedIncomingShares> {
        onBlocking {
            invoke(
                any()
            )
        }.thenReturn(shareDataList)
    }
    private val initialFinishActivityValue = false
    private val monitorFinishActivity = MutableStateFlow(initialFinishActivityValue)
    private val monitorVerificationStatus = MutableStateFlow<VerificationStatus>(
        UnVerified(
            canRequestUnblockSms = false,
            canRequestOptInVerification = false
        )
    )
    private val check2FADialog = mock<Check2FADialog>()
    private val setLatestTargetPath = mock<SetLatestTargetPath>()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = ManagerViewModel(
            monitorNodeUpdates = { monitorNodeUpdates },
            monitorContactUpdates = { monitorContactUpdates },
            monitorGlobalUpdates = { monitorGlobalUpdates },
            monitorContactRequestUpdates = { monitorContactRequestUpdates },
            getNumUnreadUserAlerts = getNumUnreadUserAlerts,
            hasInboxChildren = hasInboxChildren,
            sendStatisticsMediaDiscovery = sendStatisticsMediaDiscovery,
            savedStateHandle = savedStateHandle,
            monitorMyAvatarFile = monitorMyAvatarFile,
            getInboxNode = getInboxNode,
            monitorStorageStateEvent = monitorStorageState,
            monitorViewType = monitorViewType,
            getPrimarySyncHandle = getPrimarySyncHandle,
            getSecondarySyncHandle = getSecondarySyncHandle,
            checkCameraUpload = checkCameraUpload,
            getCloudSortOrder = getCloudSortOrder,
            monitorConnectivity = monitorConnectivity,
            broadcastUploadPauseState = mock(),
            getExtendedAccountDetail = mock(),
            getPricing = mock(),
            getFullAccountInfo = mock(),
            getActiveSubscription = mock(),
            getFeatureFlagValue = getFeatureFlagValue,
            getUnverifiedIncomingShares = getUnverifiedIncomingShares,
            getUnverifiedOutgoingShares = getUnverifiedOutgoingShares,
            monitorFinishActivity = { monitorFinishActivity },
            monitorVerificationStatus = { monitorVerificationStatus },
            check2FADialog = check2FADialog,
            setLatestTargetPath = setLatestTargetPath
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.isFirstNavigationLevel).isTrue()
            assertThat(initial.sharesTab).isEqualTo(SharesTab.INCOMING_TAB)
            assertThat(initial.transfersTab).isEqualTo(TransfersTab.NONE)
            assertThat(initial.isFirstLogin).isFalse()
            assertThat(initial.shouldSendCameraBroadcastEvent).isFalse()
            assertThat(initial.shouldStopCameraUpload).isFalse()
            assertThat(initial.nodeUpdateReceived).isFalse()
            assertThat(initial.shouldAlertUserAboutSecurityUpgrade).isFalse()
        }
    }

    @Test
    fun `test that is first navigation level is updated if new value provided`() = runTest {
        underTest.state.map { it.isFirstNavigationLevel }.distinctUntilChanged()
            .test {
                val newValue = false
                assertThat(awaitItem()).isEqualTo(true)
                underTest.setIsFirstNavigationLevel(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that shares tab is updated if new value provided`() = runTest {
        underTest.state.map { it.sharesTab }.distinctUntilChanged()
            .test {
                val newValue = SharesTab.OUTGOING_TAB
                assertThat(awaitItem()).isEqualTo(SharesTab.INCOMING_TAB)
                underTest.setSharesTab(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that transfers tab is updated if new value provided`() = runTest {
        underTest.state.map { it.transfersTab }.distinctUntilChanged()
            .test {
                val newValue = TransfersTab.PENDING_TAB
                assertThat(awaitItem()).isEqualTo(TransfersTab.NONE)
                underTest.setTransfersTab(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that user alert updates live data is not set when no updates triggered from use case`() =
        runTest {
            underTest.updateUserAlerts.test().assertNoValue()
        }

    @Test
    fun `test that contact request updates live data is not set when no updates triggered from use case`() =
        runTest {
            underTest.updateContactsRequests.test().assertNoValue()
        }

    @Test
    fun `test that user alert updates live data is set when user alert updates triggered from use case`() =
        runTest {
            val testObserver = underTest.updateUserAlerts.test()
            testObserver.assertNoValue()
            val userAlert = mock<MegaUserAlert>()
            monitorGlobalUpdates.emit(
                GlobalUpdate.OnUserAlertsUpdate(
                    userAlerts = arrayListOf(userAlert)
                )
            )
            testScheduler.advanceUntilIdle()
            testObserver.assertValue { it.getContentIfNotHandled()?.size == 1 }
        }

    @Test
    fun `test that user alert updates live data is not set when user alert updates triggered from use case with null`() =
        runTest {
            val testObserver = underTest.updateUserAlerts.test()

            monitorGlobalUpdates.emit(
                GlobalUpdate.OnUserAlertsUpdate(null)
            )
            testScheduler.advanceUntilIdle()

            testObserver.assertNoValue()
        }

    @Test
    fun `test that contact request updates live data is set when contact request updates triggered from use case`() =
        runTest {
            val testObserver = underTest.updateContactsRequests.test()
            monitorContactRequestUpdates.emit(
                listOf(
                    ContactRequest(
                        handle = 1L,
                        sourceEmail = "",
                        sourceMessage = null,
                        targetEmail = "",
                        creationTime = 1L,
                        modificationTime = 1L,
                        status = ContactRequestStatus.Unresolved,
                        isOutgoing = false,
                        isAutoAccepted = false
                    )
                )
            )
            testScheduler.advanceUntilIdle()

            testObserver.assertValue { it.getContentIfNotHandled()?.size == 1 }
        }

    @Test
    fun `test that contact request updates live data is not set when contact request updates triggered from use case with null`() =
        runTest {
            underTest.updateContactsRequests.test().assertNoValue()
        }

    @Test
    fun `test that saved initial state values are returned`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map {
            it.isFirstLogin
        }.test {
            assertThat(awaitItem()).isEqualTo(initialIsFirsLoginValue)
        }
    }

    @Test
    fun `test that is first login is updated if new boolean is provided`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.isFirstLogin }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isEqualTo(initialIsFirsLoginValue)
                underTest.setIsFirstLogin(!initialIsFirsLoginValue)
                assertThat(awaitItem()).isEqualTo(!initialIsFirsLoginValue)
            }
    }

    @Test
    fun `test that get order returns cloud sort order`() = runTest {

        val expected = SortOrder.ORDER_MODIFICATION_DESC
        whenever(getCloudSortOrder()).thenReturn(expected)
        assertThat(underTest.getOrder()).isEqualTo(expected)
    }

    @Test
    fun `test that updateToolbarTitle is true when a node update occurs`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.nodeUpdateReceived }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            monitorNodeUpdates.emit(NodeUpdate(emptyMap()))
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that updateToolbarTitle is set when calling setUpdateToolbar`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.nodeUpdateReceived }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            monitorNodeUpdates.emit(NodeUpdate(emptyMap()))
            testScheduler.advanceUntilIdle()
            assertThat(awaitItem()).isTrue()
            underTest.nodeUpdateHandled()
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that pending actions count matches incoming and outgoing shares size`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.pendingActionsCount }.distinctUntilChanged().test {
            assertThat(awaitItem()).isEqualTo(shareDataList.size * 2)
        }
    }

    @Test
    fun `test that monitor finish activity event value matches the initial value returned by use case`() =
        runTest {
            underTest.monitorFinishActivityEvent.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(initialFinishActivityValue)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitor finish activity events are returned`() =
        runTest {
            underTest.monitorFinishActivityEvent.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(initialFinishActivityValue)
                monitorFinishActivity.emit(!initialFinishActivityValue)
                assertThat(awaitItem()).isEqualTo(!initialFinishActivityValue)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test when check2FADialog returns false show2FADialog flow updated to false`() = runTest {
        whenever(check2FADialog(newAccount = false, firstLogin = true)).thenReturn(false)
        underTest.checkToShow2FADialog(newAccount = false, firstLogin = true)
        underTest.state.map { it }.distinctUntilChanged().test {
            assertThat(awaitItem().show2FADialog).isFalse()
        }
    }

    @Test
    fun `test when check2FADialog returns true show2FADialog flow updated to true`() = runTest {
        whenever(check2FADialog(newAccount = false, firstLogin = true)).thenReturn(true)
        underTest.checkToShow2FADialog(newAccount = false, firstLogin = true)
        testScheduler.advanceUntilIdle()
        underTest.state.map { it }.distinctUntilChanged().test {
            assertThat(awaitItem().show2FADialog).isTrue()
        }
    }

    @Test
    fun `test that canVerifyPhoneNumber matches canRequestOptInVerification if unverified`() {
        val expectedCanVerify = true
        runTest {
            monitorVerificationStatus.emit(
                UnVerified(
                    canRequestUnblockSms = false,
                    canRequestOptInVerification = expectedCanVerify
                )
            )
            testScheduler.advanceUntilIdle()

            underTest.state.test {
                assertThat(awaitItem().canVerifyPhoneNumber).isEqualTo(expectedCanVerify)
            }
        }
    }

    @Test
    fun `test that canVerifyPhoneNumber is false if a phone number has already been verified`() =
        runTest {
            monitorVerificationStatus.emit(
                Verified(
                    phoneNumber = VerifiedPhoneNumber.PhoneNumber("766543"),
                    canRequestUnblockSms = false,
                    canRequestOptInVerification = true
                )
            )
            testScheduler.advanceUntilIdle()

            underTest.state.test {
                assertThat(awaitItem().canVerifyPhoneNumber).isFalse()
            }
        }
}
