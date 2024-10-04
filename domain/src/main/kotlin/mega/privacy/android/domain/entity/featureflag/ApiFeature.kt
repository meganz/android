package mega.privacy.android.domain.entity.featureflag

import mega.privacy.android.domain.entity.Feature

/**
 * Remote feature
 */
interface ApiFeature : Feature {
    /**
     * Name of the Api flag
     */
    val experimentName: String

    /**
     * whether remote value should be checked or not
     */
    val checkRemote: Boolean

    /**
     * map value to boolean
     */
    fun mapValue(input: GroupFlagTypes): Boolean = when (input) {
        GroupFlagTypes.Enabled -> true
        else -> false
    }
}
