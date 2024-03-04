package mega.privacy.android.data.mapper.environment

import android.os.PowerManager.THERMAL_STATUS_CRITICAL
import android.os.PowerManager.THERMAL_STATUS_EMERGENCY
import android.os.PowerManager.THERMAL_STATUS_LIGHT
import android.os.PowerManager.THERMAL_STATUS_MODERATE
import android.os.PowerManager.THERMAL_STATUS_NONE
import android.os.PowerManager.THERMAL_STATUS_SEVERE
import android.os.PowerManager.THERMAL_STATUS_SHUTDOWN
import mega.privacy.android.domain.entity.environment.ThermalState
import javax.inject.Inject

/**
 * Thermal state mapper
 */
internal class ThermalStateMapper @Inject constructor() {

    operator fun invoke(thermalStatus: Int): ThermalState =
        when (thermalStatus) {
            THERMAL_STATUS_NONE -> ThermalState.ThermalStateNone
            THERMAL_STATUS_LIGHT -> ThermalState.ThermalStateLight
            THERMAL_STATUS_MODERATE -> ThermalState.ThermalStateModerate
            THERMAL_STATUS_SEVERE -> ThermalState.ThermalStateSevere
            THERMAL_STATUS_CRITICAL -> ThermalState.ThermalStateCritical
            THERMAL_STATUS_EMERGENCY -> ThermalState.ThermalStateEmergency
            THERMAL_STATUS_SHUTDOWN -> ThermalState.ThermalStateShutdown
            else -> ThermalState.ThermalStateNone
        }
}
