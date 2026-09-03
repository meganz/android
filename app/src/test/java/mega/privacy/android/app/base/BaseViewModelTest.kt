package mega.privacy.android.app.base

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.base.BaseViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.business.MonitorBusinessAccountExpiredUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BaseViewModelTest {

    private lateinit var underTest: BaseViewModel


    private val monitorAccountBlockedUseCase = mock<MonitorAccountBlockedUseCase>()
    private val monitorBusinessAccountExpiredUseCase = mock<MonitorBusinessAccountExpiredUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()


    @BeforeAll
    fun setup() {
        baseStubbing()
        initUnderTest()
    }

    @BeforeEach
    fun cleanUp() {
        resetMocks()
        baseStubbing()
    }

    private fun resetMocks() {
        reset(
            monitorAccountBlockedUseCase,
            monitorBusinessAccountExpiredUseCase,
            monitorAccountDetailUseCase,
        )
    }

    private fun baseStubbing() {
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        whenever(monitorBusinessAccountExpiredUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountDetailUseCase()).thenReturn(emptyFlow())
    }

    private fun initUnderTest() {
        underTest = BaseViewModel(
            monitorAccountBlockedUseCase,
            monitorBusinessAccountExpiredUseCase,
            monitorAccountDetailUseCase,
        )
    }

    @Test
    fun `test that the ui state is updated when business account expired event is received`() =
        runTest {
            val flow = flow {
                emit(Unit)
                awaitCancellation()
            }
            whenever(monitorBusinessAccountExpiredUseCase()).thenReturn(flow)
            initUnderTest() //we need to init it after stubbing because the flow is created in the initialization
            underTest.state.test {
                assertThat(awaitItem().showExpiredBusinessAlert).isTrue()
            }
        }

    @Test
    fun `test that the ui state is updated with the account type when account detail is received`() =
        runTest {
            val levelDetail = AccountLevelDetail(
                accountType = AccountType.PRO_FLEXI,
                subscriptionStatus = null,
                subscriptionRenewTime = 0L,
                accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                proExpirationTime = 0L,
                accountPlanDetail = null,
                accountSubscriptionDetailList = emptyList(),
            )
            val flow = flow {
                emit(AccountDetail(levelDetail = levelDetail))
                awaitCancellation()
            }
            whenever(monitorAccountDetailUseCase()).thenReturn(flow)
            initUnderTest() //we need to init it after stubbing because the flow is created in the initialization
            underTest.state.test {
                assertThat(awaitItem().accountType).isEqualTo(AccountType.PRO_FLEXI)
            }
        }

    @Test
    fun `test that the ui state is updated when on show expired business alert consumed is invoked`() =
        runTest {
            val flow = flow {
                emit(Unit)
                awaitCancellation()
            }
            whenever(monitorBusinessAccountExpiredUseCase()).thenReturn(flow)
            initUnderTest() //we need to init it after stubbing because the flow is created in the initialization
            underTest.state.test {
                awaitItem()
                underTest.onShowExpiredBusinessAlertConsumed()
                assertThat(awaitItem().showExpiredBusinessAlert).isFalse()
            }
        }
}