package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaStringMap
import javax.inject.Inject

/**
 * Map [MegaStringMap] to [Map]
 */
internal class MegaStringMapMapper @Inject constructor() {

    /**
     * Convert [MegaStringMap] to [Map]
     *
     * @param megaStringMap [MegaStringMap]
     * @return              [Map]
     */
    operator fun invoke(
        megaStringMap: MegaStringMap,
    ): Map<String, String> =
        mutableMapOf<String, String>().apply {
            val keys = megaStringMap.keys
            for (i in 0 until keys.size()) {
                val key = keys.get(i)
                val value = megaStringMap.get(key)
                set(key, value)
            }
        }

    /**
     * Convert [Map] to [MegaStringMap]
     *
     * @param map   [Map]
     * @return      [MegaStringMap]
     */
    operator fun invoke(map: Map<String, String>): MegaStringMap =
        MegaStringMap.createInstance().apply {
            map.forEach { (key, value) ->
                set(key, value)
            }
        }
}
