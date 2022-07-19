package test.mega.privacy.android.app.presentation.featureflag

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.ShakeDetectorUseCase
import mega.privacy.android.app.domain.usecase.VibrateDeviceUseCase
import mega.privacy.android.app.presentation.featureflag.ShakeDetectorViewModel
import mega.privacy.android.app.presentation.featureflag.model.ShakeEvent
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class ShakeDetectorViewModelTest {

    private lateinit var underTest: ShakeDetectorViewModel
    private val shakeDetectorUseCase = mock<ShakeDetectorUseCase> {
        on { invoke() }.thenReturn(flowOf(ShakeEvent(1.1F, 2.2F, 3.3F)))
    }
    private val vibrateDeViceUseCase = mock<VibrateDeviceUseCase>()
    private val scheduler = TestCoroutineScheduler()
    private val standardDispatcher = StandardTestDispatcher(scheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(standardDispatcher)
        underTest = ShakeDetectorViewModel(
            ioDispatcher = standardDispatcher,
            vibrateDeviceUseCase = vibrateDeViceUseCase,
            shakeDetectorUseCase = shakeDetectorUseCase,)
    }

    @Test
    fun `test that shake detector use case is called`() {
        underTest.registerAndCatchShakeEvent()
        runTest {
            verify(shakeDetectorUseCase, times(1))
            scheduler.advanceUntilIdle()
            shakeDetectorUseCase().collect {
                verify(vibrateDeViceUseCase, times(1))
            }
        }
    }
}