package mega.privacy.android.data.mapper.settings

import mega.privacy.android.domain.entity.settings.cookie.CookieType
import java.util.BitSet
import javax.inject.Inject

internal class CookieSettingsIntMapper @Inject constructor() {

    /**
     * Converts a Set of [CookieType] to a decimal number
     *
     * @param cookieSettings
     */
    operator fun invoke(cookieSettings: Set<CookieType>): Int {
        val bitSet = BitSet(CookieType.entries.size).apply {
            this[CookieType.ESSENTIAL.value] = true // Essential cookies are always enabled
        }

        cookieSettings.forEach { cookie ->
            bitSet[cookie.value] = true
        }

        return bitSet.toLongArray().first().toInt()
    }
}