package mega.privacy.android.app.presentation.featureflag.model

/**
 * Data class to hold feature flag information
 *
 * @property featureName
 * @property isEnabled
 */
data class FeatureFlag(
    val featureName: String,
    val description: String,
    val isEnabled: Boolean,
)
