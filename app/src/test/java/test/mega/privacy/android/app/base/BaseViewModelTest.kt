package test.mega.privacy.android.app.base

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.base.BaseViewModel
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.business.MonitorBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersFinishedUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BaseViewModelTest {

    private lateinit var underTest: BaseViewModel

    private val monitorTransfersFinishedUseCase = mock<MonitorTransfersFinishedUseCase>()
    private val monitorAccountBlockedUseCase = mock<MonitorAccountBlockedUseCase>()
    private val monitorBusinessAccountExpiredUseCase = mock<MonitorBusinessAccountExpiredUseCase>()


    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initUnderTest()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorBusinessAccountExpiredUseCase,
            monitorAccountBlockedUseCase,
            monitorBusinessAccountExpiredUseCase,
        )
    }

    private fun initUnderTest() {
        underTest = BaseViewModel(
            monitorTransfersFinishedUseCase,
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