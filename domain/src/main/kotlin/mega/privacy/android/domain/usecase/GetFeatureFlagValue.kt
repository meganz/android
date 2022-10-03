package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.Feature

/**
 * Get feature flag value
 *
 */
fun interface GetFeatureFlagValue {
    /**
     * Invoke
     *
     * @param feature
     * @return value of the feature flag or false if no value found
     */
    suspend operator fun invoke(feature: Feature): Boolean
}