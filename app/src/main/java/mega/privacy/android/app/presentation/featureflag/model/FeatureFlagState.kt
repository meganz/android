package mega.privacy.android.app.presentation.featureflag.model

import mega.privacy.android.app.domain.entity.FeatureFlag

data class FeatureFlagState(val featureFlagList: MutableList<FeatureFlag>? = null)
