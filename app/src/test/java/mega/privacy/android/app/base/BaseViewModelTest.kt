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
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
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
        )
    }

    private fun baseStubbing() {
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        whenever(monitorBusinessAccountExpiredUseCase()).thenReturn(emptyFlow())
    }

    private fun initUnderTest() {
        underTest = BaseViewModel(
            monitorAccountBlockedUseCase,
            monitorBusinessAccountExpiredUseCase,
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