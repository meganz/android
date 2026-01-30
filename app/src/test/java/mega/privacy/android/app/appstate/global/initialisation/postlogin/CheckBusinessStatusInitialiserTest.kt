package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsMasterBusinessAccountUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import mega.privacy.android.navigation.destination.BusinessExpiredAlertNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckBusinessStatusInitialiserTest {
    private lateinit var underTest: CheckBusinessStatusInitialiser

    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val monitorUpdateUserDataUseCase = mock<MonitorUpdateUserDataUseCase>()
    private val isMasterBusinessAccountUseCase = mock<IsMasterBusinessAccountUseCase>()
    private val navigationEventQueue = mock<NavigationEventQueue>()
    private val appDialogsEventQueue = mock<AppDialogsEventQueue>()

    @BeforeAll
    fun setUp() {
        underTest = CheckBusinessStatusInitialiser(
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            monitorUpdateUserDataUseCase = monitorUpdateUserDataUseCase,
            isMasterBusinessAccountUseCase = isMasterBusinessAccountUseCase,
            navigationEventQueue = navigationEventQueue,
            appDialogsEventQueue = appDialogsEventQueue,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase,
            monitorUpdateUserDataUseCase,
            isMasterBusinessAccountUseCase,
            navigationEventQueue,
            appDialogsEventQueue
        )
        // Default: monitorUpdateUserDataUseCase emits Unit
        whenever(monitorUpdateUserDataUseCase()).thenReturn(flowOf(Unit))
    }

    @Test
    fun `test that no event is emitted when isFastLogin is true`() = runTest {
        underTest("session", true)

        verifyNoInteractions(monitorAccountDetailUseCase)
        verifyNoInteractions(getBusinessStatusUseCase)
        verifyNoInteractions(isMasterBusinessAccountUseCase)
        verifyNoInteractions(navigationEventQueue)
        verifyNoInteractions(appDialogsEventQueue)
    }

    @Test
    fun `test that no event is emitted when account detail has no level detail`() = runTest {
        val accountDetail = AccountDetail(levelDetail = null)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest("session", false)

        verify(monitorAccountDetailUseCase).invoke()
        verify(monitorUpdateUserDataUseCase).invoke()
        verifyNoInteractions(getBusinessStatusUseCase)
        verifyNoInteractions(isMasterBusinessAccountUseCase)
        verifyNoInteractions(navigationEventQueue)
        verifyNoInteractions(appDialogsEventQueue)
    }

    @Test
    fun `test that no event is emitted when account is not business or pro flexi`() = runTest {
        val accountDetail = AccountDetail(
            levelDetail = AccountLevelDetail(
                accountType = AccountType.FREE,
                subscriptionStatus = null,
                subscriptionRenewTime = 0L,
                accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                proExpirationTime = 0L,
                accountPlanDetail = null,
                accountSubscriptionDetailList = emptyList()
            )
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)

        underTest("session", false)

        verify(monitorAccountDetailUseCase).invoke()
        verify(monitorUpdateUserDataUseCase).invoke()
        verify(getBusinessStatusUseCase).invoke()
        verifyNoInteractions(isMasterBusinessAccountUseCase)
        verifyNoInteractions(navigationEventQueue)
        verifyNoInteractions(appDialogsEventQueue)
    }

    private fun createAccountDetail(accountType: AccountType): AccountDetail {
        return AccountDetail(
            levelDetail = AccountLevelDetail(
                accountType = accountType,
                subscriptionStatus = null,
                subscriptionRenewTime = 0L,
                accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                proExpirationTime = 0L,
                accountPlanDetail = null,
                accountSubscriptionDetailList = emptyList()
            )
        )
    }

    @Test
    fun `test that navigation event is emitted when business account is expired`() = runTest {
        val accountDetail = createAccountDetail(AccountType.BUSINESS)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)

        underTest("session", false)

        verify(monitorAccountDetailUseCase).invoke()
        verify(monitorUpdateUserDataUseCase).invoke()
        verify(getBusinessStatusUseCase).invoke()
        verify(navigationEventQueue).emit(BusinessExpiredAlertNavKey)
        verifyNoInteractions(isMasterBusinessAccountUseCase)
        verifyNoInteractions(appDialogsEventQueue)
    }

    @Test
    fun `test that navigation event is emitted when pro flexi account is expired`() = runTest {
        val accountDetail = createAccountDetail(AccountType.PRO_FLEXI)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)

        underTest("session", false)

        verify(monitorAccountDetailUseCase).invoke()
        verify(monitorUpdateUserDataUseCase).invoke()
        verify(getBusinessStatusUseCase).invoke()
        verify(navigationEventQueue).emit(BusinessExpiredAlertNavKey)
        verifyNoInteractions(isMasterBusinessAccountUseCase)
        verifyNoInteractions(appDialogsEventQueue)
    }

    @Test
    fun `test that no dialog event is emitted when business account is in grace period but is not master business account`() =
        runTest {
            val accountDetail = createAccountDetail(AccountType.BUSINESS)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.GracePeriod)
            whenever(isMasterBusinessAccountUseCase()).thenReturn(false)

            underTest("session", false)

            verify(monitorAccountDetailUseCase).invoke()
            verify(monitorUpdateUserDataUseCase).invoke()
            verify(getBusinessStatusUseCase).invoke()
            verify(isMasterBusinessAccountUseCase).invoke()
            verifyNoInteractions(appDialogsEventQueue)
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that no dialog event is emitted when pro flexi account is in grace period`() = runTest {
        val accountDetail = createAccountDetail(AccountType.PRO_FLEXI)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.GracePeriod)

        underTest("session", false)

        verify(monitorAccountDetailUseCase).invoke()
        verify(monitorUpdateUserDataUseCase).invoke()
        verify(getBusinessStatusUseCase).invoke()
        verifyNoInteractions(isMasterBusinessAccountUseCase)
        verifyNoInteractions(appDialogsEventQueue)
        verifyNoInteractions(navigationEventQueue)
    }

    @Test
    fun `test that no event is emitted when business account is active`() = runTest {
        val accountDetail = createAccountDetail(AccountType.BUSINESS)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)

        underTest("session", false)

        verify(monitorAccountDetailUseCase).invoke()
        verify(monitorUpdateUserDataUseCase).invoke()
        verify(getBusinessStatusUseCase).invoke()
        verifyNoInteractions(isMasterBusinessAccountUseCase)
        verifyNoInteractions(navigationEventQueue)
        verifyNoInteractions(appDialogsEventQueue)
    }

    @Test
    fun `test that no event is emitted when business account is inactive`() = runTest {
        val accountDetail = createAccountDetail(AccountType.BUSINESS)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Inactive)

        underTest("session", false)

        verify(monitorAccountDetailUseCase).invoke()
        verify(monitorUpdateUserDataUseCase).invoke()
        verify(getBusinessStatusUseCase).invoke()
        verifyNoInteractions(isMasterBusinessAccountUseCase)
        verifyNoInteractions(navigationEventQueue)
        verifyNoInteractions(appDialogsEventQueue)
    }

    @Test
    fun `test that exception is handled gracefully when monitorAccountDetailUseCase throws exception`() =
        runTest {
            whenever(monitorAccountDetailUseCase()).thenReturn(
                flow { throw RuntimeException("Test error") }
            )

            // Should not throw exception
            underTest("session", false)

            verify(monitorAccountDetailUseCase).invoke()
            verify(monitorUpdateUserDataUseCase).invoke()
            verifyNoInteractions(getBusinessStatusUseCase)
            verifyNoInteractions(isMasterBusinessAccountUseCase)
            verifyNoInteractions(navigationEventQueue)
            verifyNoInteractions(appDialogsEventQueue)
        }

    @Test
    fun `test that exception is handled gracefully when getBusinessStatusUseCase throws exception`() =
        runTest {
            val accountDetail = createAccountDetail(AccountType.BUSINESS)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
            whenever(getBusinessStatusUseCase()).thenThrow(RuntimeException("Test error"))

            // Should not throw exception
            underTest("session", false)

            verify(monitorAccountDetailUseCase).invoke()
            verify(monitorUpdateUserDataUseCase).invoke()
            verify(getBusinessStatusUseCase).invoke()
            verifyNoInteractions(isMasterBusinessAccountUseCase)
            verifyNoInteractions(navigationEventQueue)
            verifyNoInteractions(appDialogsEventQueue)
        }

    @Test
    fun `test that exception is handled gracefully when isMasterBusinessAccountUseCase throws exception`() =
        runTest {
            val accountDetail = createAccountDetail(AccountType.BUSINESS)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.GracePeriod)
            whenever(isMasterBusinessAccountUseCase()).thenThrow(RuntimeException("Test error"))

            // Should not throw exception
            underTest("session", false)

            verify(monitorAccountDetailUseCase).invoke()
            verify(monitorUpdateUserDataUseCase).invoke()
            verify(getBusinessStatusUseCase).invoke()
            verify(isMasterBusinessAccountUseCase).invoke()
            verifyNoInteractions(navigationEventQueue)
            verifyNoInteractions(appDialogsEventQueue)
        }
}
