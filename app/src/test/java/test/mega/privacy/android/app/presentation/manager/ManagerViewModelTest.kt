package test.mega.privacy.android.app.presentation.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.CreateShareKey
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.MyAccountUpdate.Action
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.entity.verification.UnVerified
import mega.privacy.android.domain.entity.verification.VerificationStatus
import mega.privacy.android.domain.entity.verification.Verified
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.CheckCameraUpload
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFullAccountInfo
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlerts
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.HasInboxChildren
import mega.privacy.android.domain.usecase.MonitorOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.GetIncomingContactRequestsUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.RequireTwoFactorAuthenticationUseCase
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.MonitorFinishActivityUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.photos.mediadiscovery.SendStatisticsMediaDiscoveryUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.shares.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.shares.GetUnverifiedOutgoingShares
import mega.privacy.android.domain.usecase.transfer.DeleteOldestCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadUseCase
import nz.mega.sdk.MegaUserAlert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
class ManagerViewModelTest {
    private lateinit var underTest: ManagerViewModel

    private val monitorGlobalUpdates = MutableStateFlow<GlobalUpdate>(GlobalUpdate.OnReloadNeeded)
    private val monitorNodeUpdates = MutableSharedFlow<NodeUpdate>()
    private val monitorContactUpdates = MutableSharedFlow<UserUpdate>()
    private val monitorSecurityUpgradeInApp = MutableStateFlow(false)
    private val getNumUnreadUserAlerts =
        mock<GetNumUnreadUserAlerts> { onBlocking { invoke() }.thenReturn(0) }
    private val hasInboxChildren =
        mock<HasInboxChildren> { onBlocking { invoke() }.thenReturn(false) }
    private val monitorContactRequestUpdates = MutableStateFlow(emptyList<ContactRequest>())

    private val initialIsFirsLoginValue = true
    private val sendStatisticsMediaDiscoveryUseCase = mock<SendStatisticsMediaDiscoveryUseCase>()
    private val savedStateHandle = SavedStateHandle(
        mapOf(
            ManagerViewModel.isFirstLoginKey to initialIsFirsLoginValue
        )
    )
    private val getInboxNode = mock<GetInboxNode> { onBlocking { invoke() }.thenReturn(null) }
    private val monitorStorageState = mock<MonitorStorageStateEventUseCase> {
        onBlocking { invoke() }.thenReturn(
            MutableStateFlow(
                StorageStateEvent(
                    handle = 0L,
                    eventString = "",
                    number = 0L,
                    text = "",
                    type = EventType.Storage,
                    storageState = StorageState.Unknown,
                )
            )
        )
    }
    private val monitorViewType = mock<MonitorViewType> {
        onBlocking { invoke() }.thenReturn(
            flow { awaitCancellation() })
    }
    private val getPrimarySyncHandleUseCase = mock<GetPrimarySyncHandleUseCase>()
    private val getSecondarySyncHandleUseCase = mock<GetSecondarySyncHandleUseCase>()
    private val checkCameraUpload = mock<CheckCameraUpload>()
    private val getCloudSortOrder =
        mock<GetCloudSortOrder> { onBlocking { invoke() }.thenReturn(SortOrder.ORDER_ALPHABETICAL_ASC) }
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val getFeatureFlagValueUseCase =
        mock<GetFeatureFlagValueUseCase> { onBlocking { invoke(any()) }.thenReturn(false) }
    private val shareDataList = listOf(
        ShareData(
            user = "user",
            nodeHandle = 8766L,
            access = AccessPermission.READ,
            timeStamp = 987654678L,
            isPending = true,
            isVerified = false,
        ),
        ShareData(
            user = "user",
            nodeHandle = 8766L,
            access = AccessPermission.READ,
            timeStamp = 987654678L,
            isPending = true,
            isVerified = false,
        )
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
    private val monitorFinishActivity = mock<MonitorFinishActivityUseCase> {
        onBlocking { invoke() }.thenReturn(
            flowOf(
                initialFinishActivityValue,
                !initialFinishActivityValue
            )
        )
    }
    private val monitorVerificationStatus = MutableStateFlow<VerificationStatus>(
        UnVerified(
            canRequestUnblockSms = false,
            canRequestOptInVerification = false,
        )
    )

    private val monitorPushNotificationSettingsUpdate =
        mock<MonitorUpdatePushNotificationSettingsUseCase> {
            onBlocking { invoke() }.thenReturn(flowOf(true))
        }
    private val requireTwoFactorAuthenticationUseCase =
        mock<RequireTwoFactorAuthenticationUseCase>()
    private val setCopyLatestTargetPathUseCase = mock<SetCopyLatestTargetPathUseCase>()
    private val setMoveLatestTargetPathUseCase = mock<SetMoveLatestTargetPathUseCase>()
    private val monitorUserUpdates = mock<MonitorUserUpdates>()
    private val monitorMyAccountUpdateUseCase = mock<MonitorMyAccountUpdateUseCase> {
        onBlocking { invoke() }.thenReturn(
            flowOf(
                MyAccountUpdate(Action.STORAGE_STATE_CHANGED, StorageState.Green),
                MyAccountUpdate(Action.UPDATE_ACCOUNT_DETAILS, null)
            )
        )
    }
    private val establishCameraUploadsSyncHandlesUseCase =
        mock<EstablishCameraUploadsSyncHandlesUseCase>()
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()
    private val stopCameraUploadUseCase = mock<StopCameraUploadUseCase>()
    private val createShareKey = mock<CreateShareKey>()
    private val deleteOldestCompletedTransfersUseCase =
        mock<DeleteOldestCompletedTransfersUseCase>()
    private val monitorOfflineNodeAvailabilityUseCase =
        mock<MonitorOfflineFileAvailabilityUseCase>()
    private val getIncomingContactRequestUseCase = mock<GetIncomingContactRequestsUseCase>()

    private val getPricing = mock<GetPricing>()
    private val getFullAccountInfo = mock<GetFullAccountInfo>()

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
            sendStatisticsMediaDiscoveryUseCase = sendStatisticsMediaDiscoveryUseCase,
            savedStateHandle = savedStateHandle,
            getInboxNode = getInboxNode,
            monitorStorageStateEventUseCase = monitorStorageState,
            monitorViewType = monitorViewType,
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
            getSecondarySyncHandleUseCase = getSecondarySyncHandleUseCase,
            checkCameraUpload = checkCameraUpload,
            getCloudSortOrder = getCloudSortOrder,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            broadcastUploadPauseState = mock(),
            getExtendedAccountDetail = mock(),
            getPricing = getPricing,
            getFullAccountInfo = getFullAccountInfo,
            getActiveSubscription = mock(),
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getUnverifiedIncomingShares = getUnverifiedIncomingShares,
            getUnverifiedOutgoingShares = getUnverifiedOutgoingShares,
            monitorFinishActivityUseCase = monitorFinishActivity,
            monitorVerificationStatus = { monitorVerificationStatus },
            requireTwoFactorAuthenticationUseCase = requireTwoFactorAuthenticationUseCase,
            setCopyLatestTargetPathUseCase = setCopyLatestTargetPathUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            monitorSecurityUpgradeInApp = { monitorSecurityUpgradeInApp },
            listenToNewMediaUseCase = mock(),
            monitorUserUpdates = monitorUserUpdates,
            monitorMyAccountUpdateUseCase = monitorMyAccountUpdateUseCase,
            establishCameraUploadsSyncHandlesUseCase = establishCameraUploadsSyncHandlesUseCase,
            monitorUpdatePushNotificationSettingsUseCase = monitorPushNotificationSettingsUpdate,
            startCameraUploadUseCase = startCameraUploadUseCase,
            stopCameraUploadUseCase = stopCameraUploadUseCase,
            createShareKey = createShareKey,
            saveContactByEmailUseCase = mock(),
            deleteOldestCompletedTransfersUseCase = deleteOldestCompletedTransfersUseCase,
            monitorOfflineNodeAvailabilityUseCase = monitorOfflineNodeAvailabilityUseCase,
            getIncomingContactRequestsUseCase = getIncomingContactRequestUseCase
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
                        isAutoAccepted = false,
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
                whenever(monitorFinishActivity()).thenReturn(flowOf(!initialFinishActivityValue))
                assertThat(awaitItem()).isEqualTo(initialFinishActivityValue)
                monitorFinishActivity()
                assertThat(awaitItem()).isEqualTo(!initialFinishActivityValue)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitor my account update events are returned`() =
        runTest {
            underTest.monitorMyAccountUpdateEvent.distinctUntilChanged().test {
                assertThat(awaitItem().action).isEqualTo(Action.STORAGE_STATE_CHANGED)
                monitorFinishActivity()
                assertThat(awaitItem().action).isEqualTo(Action.UPDATE_ACCOUNT_DETAILS)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test when check2FADialog returns false show2FADialog flow updated to false`() = runTest {
        whenever(
            requireTwoFactorAuthenticationUseCase(
                newAccount = false,
                firstLogin = true
            )
        ).thenReturn(false)
        underTest.checkToShow2FADialog(newAccount = false, firstLogin = true)
        underTest.state.map { it }.distinctUntilChanged().test {
            assertThat(awaitItem().show2FADialog).isFalse()
        }
    }

    @Test
    fun `test when check2FADialog returns true show2FADialog flow updated to true`() = runTest {
        whenever(
            requireTwoFactorAuthenticationUseCase(
                newAccount = false,
                firstLogin = true
            )
        ).thenReturn(true)
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
                    canRequestOptInVerification = expectedCanVerify,
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

    @Test
    fun `test that the camera uploads sync handles are established when receiving an update to change the camera uploads folders`() =
        runTest {
            val userUpdates = listOf(
                UserChanges.Birthday,
                UserChanges.CameraUploadsFolder,
                UserChanges.Country,
            )

            whenever(monitorUserUpdates()).thenReturn(userUpdates.asFlow())
            testScheduler.advanceUntilIdle()

            verify(establishCameraUploadsSyncHandlesUseCase).invoke()
        }

    @Test
    fun `test that when push notification settings is updated state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorPushNotificationSettingsUpdate).invoke()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isPushNotificationSettingsUpdatedEvent).isTrue()
            }
        }

    @Test
    fun `test that when startCameraUpload is called, startCameraUploadUseCase is called`() =
        runTest {
            underTest.startCameraUpload()
            testScheduler.advanceUntilIdle()
            verify(startCameraUploadUseCase, times(1)).invoke()
        }

    @Test
    fun `test that when stopCameraUpload is called, stopCameraUploadUseCase is called`() =
        runTest {
            underTest.stopCameraUpload()
            testScheduler.advanceUntilIdle()
            verify(stopCameraUploadUseCase, times(1)).invoke()
        }

    @Test
    fun `test that deletion of oldest completed transfers is triggered on init`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(deleteOldestCompletedTransfersUseCase).invoke()
        }

    @Test
    fun `test that get incoming contact requests is triggered when there is contact request event`() =
        runTest {
            val testObserver = underTest.updateContactsRequests.test()
            testObserver.assertNoValue()

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
                        isAutoAccepted = false,
                    )
                )
            )
            testScheduler.advanceUntilIdle()
            verify(getIncomingContactRequestUseCase, times(2)).invoke()
        }

    @Test
    fun `test that an exception from get pricing is not propagated`() = runTest {
        whenever(getPricing(any())).thenAnswer { throw MegaException(1, "It's broken") }

        with(underTest) {
            getProductAccounts()
            state.test {
                assertThat(cancelAndConsumeRemainingEvents().any { it is Event.Error }).isFalse()
            }
        }
    }

    @Test
    fun `test that an exception from get full account info is not propagated`() = runTest {
        whenever(getFullAccountInfo()).thenAnswer { throw MegaException(1, "It's broken") }

        with(underTest) {
            askForFullAccountInfo()
            state.test {
                assertThat(cancelAndConsumeRemainingEvents().any { it is Event.Error }).isFalse()
            }
        }
    }

    @Test
    fun `test that an exception from require 2FA is not propagated`() = runTest {
        whenever(requireTwoFactorAuthenticationUseCase(newAccount = false, firstLogin = false))
            .thenAnswer { throw MegaException(1, "It's broken") }

        with(underTest) {
            checkToShow2FADialog(newAccount = false, firstLogin = false)
            state.map { it.show2FADialog }.test {
                assertFalse(awaitItem())
                assertThat(cancelAndConsumeRemainingEvents().any { it is Event.Error }).isFalse()
            }
        }
    }
}
