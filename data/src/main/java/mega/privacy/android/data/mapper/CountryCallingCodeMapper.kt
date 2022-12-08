package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaStringListMap


/**
 * Map [MegaStringListMap] to [List]
 */
typealias CountryCallingCodeMapper = (@JvmSuppressWildcards MegaStringListMap) -> List<String>

/**
 * Map [MegaStringListMap] to [List]
 */
internal fun toCountryCallingCodes(callingCodes: MegaStringListMap?): List<String> {
    return callingCodes?.let {
        val codedCountryCode = mutableListOf<String>()
        val keyList = it.keys
        for (i in 0 until keyList.size()) {
            val key = keyList[i]
            val contentBuffer = StringBuffer()
            contentBuffer.append("$key:")
            for (j in 0 until callingCodes[key].size()) {
                val dialCode = callingCodes[key][j]
                contentBuffer.append("$dialCode,")
            }
            codedCountryCode.add(contentBuffer.toString())
        }
        codedCountryCode
    } ?: emptyList()
}
