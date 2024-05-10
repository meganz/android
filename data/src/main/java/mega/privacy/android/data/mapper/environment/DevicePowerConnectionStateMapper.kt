package mega.privacy.android.data.mapper.environment

import android.content.Intent
import mega.privacy.android.domain.entity.environment.DevicePowerConnectionState
import javax.inject.Inject

/**
 * Mapper to convert a Power Type from an [Intent] into a respective [DevicePowerConnectionState]
 */
internal class DevicePowerConnectionStateMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param powerType The Power Type from an [Intent], which may be null
     * @return The mapped Device Power Connection State
     */
    operator fun invoke(powerType: String?): DevicePowerConnectionState =
        when (powerType) {
            Intent.ACTION_POWER_CONNECTED -> DevicePowerConnectionState.Connected
            Intent.ACTION_POWER_DISCONNECTED -> DevicePowerConnectionState.Disconnected
            else -> DevicePowerConnectionState.Unknown
        }
}