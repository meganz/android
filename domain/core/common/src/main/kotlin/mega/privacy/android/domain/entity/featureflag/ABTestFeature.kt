package mega.privacy.android.domain.entity.featureflag

import mega.privacy.android.domain.entity.Feature

/**
 * Remote feature
 */
interface ABTestFeature : Feature {
    val experimentName: String
    val checkRemote: Boolean
    fun mapValue(input: Long): Boolean = when (input) {
        0L -> false
        else -> true
    }
}