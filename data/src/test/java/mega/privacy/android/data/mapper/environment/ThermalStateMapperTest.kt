package mega.privacy.android.data.mapper.environment

import android.os.PowerManager.THERMAL_STATUS_CRITICAL
import android.os.PowerManager.THERMAL_STATUS_EMERGENCY
import android.os.PowerManager.THERMAL_STATUS_LIGHT
import android.os.PowerManager.THERMAL_STATUS_MODERATE
import android.os.PowerManager.THERMAL_STATUS_NONE
import android.os.PowerManager.THERMAL_STATUS_SEVERE
import android.os.PowerManager.THERMAL_STATUS_SHUTDOWN
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.environment.ThermalState
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class ThermalStateMapperTest {
    private val underTest = ThermalStateMapper()

    @TestFactory
    fun `test that ThermalState is mapped correctly`() =
        HashMap<Int, ThermalState>().apply {
            put(THERMAL_STATUS_NONE, ThermalState.ThermalStateNone)
            put(THERMAL_STATUS_LIGHT, ThermalState.ThermalStateLight)
            put(THERMAL_STATUS_MODERATE, ThermalState.ThermalStateModerate)
            put(THERMAL_STATUS_SEVERE, ThermalState.ThermalStateSevere)
            put(THERMAL_STATUS_CRITICAL, ThermalState.ThermalStateCritical)
            put(THERMAL_STATUS_EMERGENCY, ThermalState.ThermalStateEmergency)
            put(THERMAL_STATUS_SHUTDOWN, ThermalState.ThermalStateShutdown)
            put(7, ThermalState.ThermalStateNone) // random Int
        }.map { (thermalStatus, thermalState) ->
            DynamicTest.dynamicTest("test that $thermalStatus is mapped to $thermalState") {
                assertThat(underTest(thermalStatus)).isEqualTo(thermalState)
            }
        }
}
