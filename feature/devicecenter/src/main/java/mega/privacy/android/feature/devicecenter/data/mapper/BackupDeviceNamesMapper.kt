package mega.privacy.android.feature.devicecenter.data.mapper

import nz.mega.sdk.MegaStringMap
import javax.inject.Inject

/**
 * Mapper that converts a [MegaStringMap] whose Key-Value Pair is the Device ID and
 * Device Name into a [Map]
 */
internal class BackupDeviceNamesMapper @Inject constructor() {
    /**
     * Invocation function
     *
     * @param sdkStringMap A potentially nullable [MegaStringMap] containing the Key-Value Pair of the Device ID and
     * Device Name
     * @return A [Map] equivalent of the [MegaStringMap]
     */
    operator fun invoke(sdkStringMap: MegaStringMap?): Map<String, String> =
        sdkStringMap?.let { megaStringMap ->
            val keys = megaStringMap.keys
            val keySize = keys.size()

            return if (keySize <= 0) emptyMap()
            else buildMap {
                for (i in 0 until keySize) {
                    val deviceId = keys[i]
                    val deviceName = megaStringMap.get(deviceId).orEmpty()
                    this[deviceId] = deviceName
                }
            }
        } ?: emptyMap()
}