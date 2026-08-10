package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.fcm.FcmManager
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NotificationTopicsInitializerTest {

    private lateinit var underTest: NotificationTopicsInitializer
    private val fcmManager: FcmManager = mock()
    private val monitorAccountDetailFakeFlow = MutableSharedFlow<AccountDetail>()

    @BeforeAll
    fun setUp() {
        val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock {
            whenever(it()).thenReturn(monitorAccountDetailFakeFlow)
        }
        underTest = NotificationTopicsInitializer(monitorAccountDetailUseCase, fcmManager)
    }

    @BeforeEach
    fun cleanUp() {
        reset(fcmManager)
    }

    @ParameterizedTest
    @EnumSource(AccountType::class)
    fun `test that fcmManager subscribes to account type topic when account detail is emitted`(
        accountType: AccountType,
    ) = runTest {
        val job = launch {
            underTest.invoke("session", false)
        }
        advanceUntilIdle()
        monitorAccountDetailFakeFlow.emit(
            AccountDetail(
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
        )

        verify(fcmManager).subscribeToAccountTypeTopic(accountType)
        verify(fcmManager).setAccountTypeUserProperty(accountType)
        job.cancel()
    }

    @Test
    fun `test that fcmManager does not subscribe when account detail has null levelDetail`() =
        runTest {
            val job = launch {
                underTest.invoke("session", false)
            }
            advanceUntilIdle()
            monitorAccountDetailFakeFlow.emit(AccountDetail(levelDetail = null))

            verifyNoInteractions(fcmManager)
            job.cancel()
        }

    @Test
    fun `test that fcmManager does not subscribe when account detail has null accountType`() =
        runTest {
            val job = launch {
                underTest.invoke("session", false)
            }
            advanceUntilIdle()
            monitorAccountDetailFakeFlow.emit(
                AccountDetail(
                    levelDetail = AccountLevelDetail(
                        accountType = null,
                        subscriptionStatus = null,
                        subscriptionRenewTime = 0L,
                        accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                        proExpirationTime = 0L,
                        accountPlanDetail = null,
                        accountSubscriptionDetailList = emptyList()
                    )
                )
            )

            verifyNoInteractions(fcmManager)
            job.cancel()
        }

    @Test
    fun `test that fcmManager subscribes only once when same account type is emitted multiple times`() =
        runTest {
            val job = launch {
                underTest.invoke("session", false)
            }
            advanceUntilIdle()
            val accountType = AccountType.FREE
            val accountDetail = AccountDetail(
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
            // Emit the same account detail three times
            monitorAccountDetailFakeFlow.emit(accountDetail)
            monitorAccountDetailFakeFlow.emit(accountDetail)
            monitorAccountDetailFakeFlow.emit(accountDetail)


            // Should only be called once due to distinctUntilChanged()
            verify(fcmManager).subscribeToAccountTypeTopic(accountType)
            verify(fcmManager).setAccountTypeUserProperty(accountType)
            job.cancel()
        }
}
