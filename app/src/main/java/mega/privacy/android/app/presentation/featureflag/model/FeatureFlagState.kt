package mega.privacy.android.app.presentation.featureflag.model

import mega.privacy.android.app.domain.entity.FeatureFlag

/**
 * Maintains a state of @FeatureFlag
 *
 * @param : List of @FeatureFlag
 */
data class FeatureFlagState(val featureFlagList: MutableList<FeatureFlag>? = null)
