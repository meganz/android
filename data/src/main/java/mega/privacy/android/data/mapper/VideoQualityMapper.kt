package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.VideoQuality

/**
 * VideoQualityMapper
 */
typealias VideoQualityMapper = (@JvmSuppressWildcards String?) -> @JvmSuppressWildcards VideoQuality?

/**
 * To video quality
 *
 * @param input
 */
fun toVideoQuality(input: String?) = when (input) {
    "0" -> VideoQuality.LOW
    "1" -> VideoQuality.MEDIUM
    "2" -> VideoQuality.HIGH
    "3" -> VideoQuality.ORIGINAL
    else -> null
}