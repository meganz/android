package test.mega.privacy.android.app.presentation.featureflag

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.featureflag.ShakeDetectorViewModel
import mega.privacy.android.app.domain.model.ShakeEvent
import mega.privacy.android.app.domain.usecase.ShakeDetector
import mega.privacy.android.app.domain.usecase.VibrateDevice
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ShakeDetectorViewModelTest {

    private lateinit var underTest: ShakeDetectorViewModel
    private val shakeDetectorUseCase = mock<ShakeDetector> {
        on { invoke() }.thenReturn(flowOf(ShakeEvent(1.1F, 2.2F, 3.3F)))
    }
    private val vibrateDeViceUseCase = mock<VibrateDevice>()
    private val scheduler = TestCoroutineScheduler()
    private val standardDispatcher = StandardTestDispatcher(scheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(standardDispatcher)
        underTest = ShakeDetectorViewModel(
            ioDispatcher = standardDispatcher,
            vibrateDevice = vibrateDeViceUseCase,
            shakeDetector = shakeDetectorUseCase,
            coroutineScope = TestScope())
    }

    @Test
    fun `test that shake detector use case is called`() {
        underTest.registerAndCatchShakeEvent()
        runTest {
            underTest.state.test {
                assertFalse(awaitItem())
            }
            verify(shakeDetectorUseCase, times(1))
            scheduler.advanceUntilIdle()
            shakeDetectorUseCase().collect {
                underTest.state.apply {
                    tryEmit(true)
                    test {
                        assertTrue(awaitItem())
                    }
                }
                verify(vibrateDeViceUseCase, times(1))
            }
        }
    }
}