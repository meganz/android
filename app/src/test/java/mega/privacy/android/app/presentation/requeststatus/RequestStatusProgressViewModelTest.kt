package mega.privacy.android.app.presentation.requeststatus

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.requeststatus.MonitorRequestStatusProgressEventUseCase
import org.junit.jupiter.api.AfterEach
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
class RequestStatusProgressViewModelTest {

    private lateinit var underTest: RequestStatusProgressViewModel

    private val monitorRequestStatusProgressEventUseCase =
        mock<MonitorRequestStatusProgressEventUseCase>()
    private val requestStatusProgressFakeFlow = MutableSharedFlow<Progress>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeEach
    fun setUp() {
        runBlocking {
            stubCommon()
        }
        initViewModel()
    }

    private fun initViewModel() {
        underTest = RequestStatusProgressViewModel(
            monitorRequestStatusProgressEventUseCase = monitorRequestStatusProgressEventUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.progress).isNull()
        }
    }

    @Test
    fun `test that progress is updated when event is received`() = runTest {
        val newProgress = 0.5f
        requestStatusProgressFakeFlow.emit(Progress(newProgress))
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.progress?.floatValue).isEqualTo(newProgress)
        }
    }

    @Test
    fun `test that progress is set to null when exception is thrown`() = runTest {
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(
            flow {
                throw Exception()
            }
        )
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.progress).isNull()
        }
    }

    private suspend fun stubCommon() {
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(
            requestStatusProgressFakeFlow
        )
        whenever(getFeatureFlagValueUseCase(AppFeatures.RequestStatusProgressDialog)).thenReturn(
            true
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(monitorRequestStatusProgressEventUseCase)
    }
}