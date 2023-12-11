package mega.privacy.android.data.mapper.settings

import mega.privacy.android.domain.entity.settings.cookie.CookieType
import java.util.BitSet
import javax.inject.Inject

internal class CookieSettingsMapper @Inject constructor() {

    /**
     * Converts a list of [CookieType] to a [BitSet]
     *
     * @param numDetails
     */
    operator fun invoke(numDetails: Int): MutableSet<CookieType> {
        val result = mutableSetOf<CookieType>()
        val bitSet = BitSet.valueOf(longArrayOf(numDetails.toLong()))

        for (i in 0..bitSet.length()) {
            if (bitSet[i]) {
                result.add(CookieType.valueOf(i))
            }
        }
        return result
    }
}