package mega.privacy.android.data.mapper.advertisements

import mega.privacy.android.domain.entity.advertisements.AdDetails
import nz.mega.sdk.MegaStringMap
import javax.inject.Inject

/**
 * Map [MegaStringMap] to [List<AdDetails>]
 */
internal class AdDetailsMapper @Inject constructor() {
    /**
     * Convert key-value pairs from [MegaStringMap] to [List<AdDetails>]
     *
     * @param megaStringMap [MegaStringMap]
     * @return              [List<AdDetails>]
     */
    operator fun invoke(megaStringMap: MegaStringMap): List<AdDetails> {
        val keys = megaStringMap.keys
        return List(keys.size()) { i ->
            val slotId = keys.get(i)
            val url = megaStringMap.get(slotId)
            AdDetails(slotId = slotId, url = url)
        }
    }
}