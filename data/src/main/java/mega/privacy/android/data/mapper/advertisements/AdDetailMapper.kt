package mega.privacy.android.data.mapper.advertisements

import mega.privacy.android.domain.entity.advertisements.AdDetail
import nz.mega.sdk.MegaStringMap
import javax.inject.Inject

/**
 * Map [MegaStringMap] to [List<AdDetail>]
 */
internal class AdDetailMapper @Inject constructor() {
    /**
     * Convert key-value pairs from [MegaStringMap] to [List<AdDetail>]
     *
     * @param megaStringMap [MegaStringMap]
     * @return              [List<AdDetail>]
     */
    operator fun invoke(megaStringMap: MegaStringMap): List<AdDetail> {
        val keys = megaStringMap.keys
        return (0 until keys.size()).map {
            val slotId = keys.get(it)
            val url = megaStringMap.get(slotId)
            AdDetail(slotId = slotId, url = url)
        }
    }
}