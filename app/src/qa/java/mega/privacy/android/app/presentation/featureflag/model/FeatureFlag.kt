package mega.privacy.android.app.presentation.featureflag.model

/**
 * Feature flag
 *
 * @property featureName
 * @property description
 * @property isEnabled
 */
data class FeatureFlag(
    val featureName: String,
    val description: String,
    val isEnabled: Boolean,
)
