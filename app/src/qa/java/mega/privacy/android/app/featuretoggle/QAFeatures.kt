package mega.privacy.android.app.featuretoggle

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

/**
 * Qa features
 *
 * @property description
 * @property defaultValue
 */
enum class QAFeatures(override val description: String, private val defaultValue: Boolean) :
    Feature {
    /**
     * QA Test toggle
     */
    QATest("This is a test toggle in QA. It displays or hides the feature flag descriptions", true);

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            values().firstOrNull { it == feature }?.defaultValue
    }
}