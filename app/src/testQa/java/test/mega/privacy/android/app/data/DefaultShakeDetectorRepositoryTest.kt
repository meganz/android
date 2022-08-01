package test.mega.privacy.android.app.data

import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.data.gateway.MotionSensorGateway
import mega.privacy.android.app.data.gateway.VibratorGateway
import mega.privacy.android.app.data.repository.DefaultShakeDetectorRepository
import mega.privacy.android.app.domain.repository.ShakeDetectorRepository
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

    private lateinit var underTest: ShakeDetectorRepository
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
        underTest.monitorShakeEvents()
        whenever(motionSensorGateway.monitorMotionEvents(any())).thenAnswer {
            (it.arguments[0] as SensorEventListener).onSensorChanged(sensorEvent)
            whenever(sensorEvent.values).thenReturn(floatArrayOf(1.1F, 2.2F, 3.3F))
            runTest {
                underTest.monitorShakeEvents().test {
                    assertEquals(1.1F, sensorEvent.values[0])
                    assertEquals(2.2F, sensorEvent.values[1])
                    assertEquals(3.3F, sensorEvent.values[2])
                }
            }
        }
    }

    @Test
    fun `test that vibrate device gets called`() {
        underTest.vibrateDevice()
        verify(vibratorGateway).vibrateDevice(any())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}