package test.mega.privacy.android.app.data

import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.data.gateway.MotionSensorGateway
import mega.privacy.android.app.data.gateway.VibratorGateway
import mega.privacy.android.app.data.repository.DefaultShakeDetectorRepository
import mega.privacy.android.app.domain.repository.ShakeDetectorRepository
import mega.privacy.android.app.domain.model.ShakeEvent
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultShakeDetectorRepositoryTest {

    lateinit var underTest: ShakeDetectorRepository
    private val vibratorGateway = mock<VibratorGateway>()
    private val motionSensorGateway = mock<MotionSensorGateway>()

    private val scheduler = TestCoroutineScheduler()
    private val standardDispatcher = StandardTestDispatcher(scheduler)
    private val sensorEvent = mock<SensorEvent>()

    @Before
    fun setUp() {
        Dispatchers.setMain(standardDispatcher)
        underTest = DefaultShakeDetectorRepository(vibratorGateway = vibratorGateway,
            motionSensorGateway = motionSensorGateway)
    }

    @Test
    fun `test that flow is returned from the sensor event listener`() {
        whenever(motionSensorGateway.monitorMotionEvents(any())).thenAnswer {
            (it.arguments[0] as SensorEventListener).onSensorChanged(sensorEvent)
        }
        whenever(underTest.monitorShakeEvents()).thenReturn(flowOf(ShakeEvent(1.1F,
            2.2F,
            3.3F)))
        runTest {
            underTest.monitorShakeEvents().test {
                val event = awaitItem()
                assertEquals(1.1F, event.x)
                assertEquals(2.2F, event.y)
                assertEquals(3.3F, event.z)
            }
        }
        verify(motionSensorGateway).monitorMotionEvents(any())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}