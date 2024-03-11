package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsConcurrentUploadsLimit
import mega.privacy.android.domain.entity.environment.ThermalState
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.environment.MonitorDeviceThermalStateUseCase
import javax.inject.Inject
import kotlin.math.min

/**
 * Monitor the number of Camera Uploads concurrent uploads limit based on the device conditions
 */
class MonitorConcurrentUploadsLimitUseCase @Inject constructor(
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase,
    private val monitorDeviceThermalStateUseCase: MonitorDeviceThermalStateUseCase,
) {
    /**
     * Invoke
     *
     * @param defaultLimit the default concurrent uploads limit
     */
    operator fun invoke(defaultLimit: Int): Flow<Int> = combine(
        monitorBatteryInfoUseCase(),
        monitorDeviceThermalStateUseCase()
    ) { batteryLevel, thermalState ->
        val concurrentUploadsBasedOnThermalState = when (thermalState) {
            ThermalState.ThermalStateNone, ThermalState.ThermalStateLight -> defaultLimit
            ThermalState.ThermalStateModerate -> CameraUploadsConcurrentUploadsLimit.ThermalStateModerate.limit
            else -> CameraUploadsConcurrentUploadsLimit.ThermalStateSevere.limit
        }
        val concurrentUploadsBasedOnBatteryLevel = when {
            batteryLevel.isCharging -> defaultLimit
            batteryLevel.level >= 75 -> defaultLimit
            batteryLevel.level >= 50 -> CameraUploadsConcurrentUploadsLimit.BatteryLevelOver50.limit
            else -> CameraUploadsConcurrentUploadsLimit.BatteryLevelOver20.limit
        }

        min(
            concurrentUploadsBasedOnThermalState,
            concurrentUploadsBasedOnBatteryLevel
        )
    }
}
