package mega.privacy.android.data.mapper

import javax.inject.Inject

/**
 *  Remote Feature Flag Mapper
 *
 *  takes [Long] value from getABTestValue method and returns [Boolean]
 */
internal class ABTestFeatureFlagValueMapper @Inject constructor() {
    /**
     *  Invoke
     *
     *  @return [Boolean]
     *  @param abTestValue
     */
    operator fun invoke(abTestValue: Long): Boolean = when (abTestValue) {
        0L -> false
        else -> true
    }
}