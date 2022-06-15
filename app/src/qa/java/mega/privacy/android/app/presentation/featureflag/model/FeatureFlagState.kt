package mega.privacy.android.app.presentation.featureflag.model

import mega.privacy.android.app.domain.entity.FeatureFlag

/**
 * Data class for Feature flag menu UI state
 */
data class FeatureFlagState(val featureFlagList: MutableList<FeatureFlag>? = null)
