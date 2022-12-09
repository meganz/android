package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.VideoQuality

/**
 * VideoQualityIntMapper
 */
typealias VideoQualityIntMapper = (@JvmSuppressWildcards VideoQuality) -> @JvmSuppressWildcards Int


/**
 * Video quality to int
 *
 * @param quality
 */
internal fun videoQualityToInt(quality: VideoQuality) = when(quality){
    VideoQuality.LOW -> 0
    VideoQuality.MEDIUM -> 1
    VideoQuality.HIGH -> 2
    VideoQuality.ORIGINAL -> 3
}