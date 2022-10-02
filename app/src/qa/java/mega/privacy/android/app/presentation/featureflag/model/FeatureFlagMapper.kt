package mega.privacy.android.app.presentation.featureflag.model

import mega.privacy.android.domain.entity.Feature

typealias FeatureFlagMapper = (@JvmSuppressWildcards Feature, @JvmSuppressWildcards Boolean) -> @JvmSuppressWildcards FeatureFlag

internal fun toFeatureFlag(
    feature: Feature,
    value: Boolean,
) = FeatureFlag(
    featureName = feature.name,
    description = feature.description,
    isEnabled = value,
)