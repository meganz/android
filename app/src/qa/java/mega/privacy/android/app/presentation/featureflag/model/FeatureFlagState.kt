package mega.privacy.android.app.presentation.featureflag.model

/**
 * Data class for Feature flag menu UI state
 *
 * @param featureFlagList = List of @FeatureFlag
 */
data class FeatureFlagState(val featureFlagList: List<FeatureFlag> = emptyList())
