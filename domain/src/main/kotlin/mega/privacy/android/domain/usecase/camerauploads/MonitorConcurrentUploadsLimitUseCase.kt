package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.environment.ThermalState
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.environment.MonitorDeviceThermalStateUseCase
import javax.inject.Inject
import kotlin.math.max
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
        monitorDeviceThermalStateUseCase(),
    ) { batteryLevel, thermalState ->
        val concurrentUploadsBasedOnBatteryLevel =
            getConcurrentUploadsBasedOnBatteryLevel(batteryLevel, defaultLimit)
        val concurrentUploadsBasedOnThermalState =
            getConcurrentUploadsBasedOnThermalState(thermalState, defaultLimit)

        min(
            concurrentUploadsBasedOnBatteryLevel,
            concurrentUploadsBasedOnThermalState,
        )
    }

    /**
     * Calculate the number of concurrent uploads based on the device thermal state
     *
     * @param thermalState the device thermal state
     * @param defaultLimit the default concurrent uploads limit
     *
     * @return an [Int] representing the number of concurrent uploads calculated based on thermal condition
     */
    private fun getConcurrentUploadsBasedOnThermalState(
        thermalState: ThermalState,
        defaultLimit: Int,
    ) = when (thermalState) {
        ThermalState.ThermalStateNone, ThermalState.ThermalStateLight -> defaultLimit
        ThermalState.ThermalStateModerate -> max(1, defaultLimit / 2)
        else -> 1
    }

    /**
     * Calculate the number of concurrent uploads based on the battery level
     *
     * @param batteryLevel the battery level
     * @param defaultLimit the default concurrent uploads limit
     *
     * @return an [Int] representing the number of concurrent uploads calculated based on battery condition
     */
    private fun getConcurrentUploadsBasedOnBatteryLevel(
        batteryLevel: BatteryInfo,
        defaultLimit: Int,
    ) = when {
        batteryLevel.isCharging -> defaultLimit
        batteryLevel.level >= 50 -> defaultLimit
        else -> max(1, defaultLimit / 2)
    }
}
