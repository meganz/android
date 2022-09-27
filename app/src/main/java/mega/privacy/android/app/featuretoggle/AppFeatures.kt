package mega.privacy.android.app.featuretoggle

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

/**
 * App features
 *
 * @property description
 * @property defaultValue
 */
enum class AppFeatures(override val description: String, private val defaultValue: Boolean) :
    Feature {
    /**
     * App Test toggle
     */
    AppTest("This is a test toggle. It does nothing", false);

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            values().firstOrNull { it == feature }?.defaultValue
    }
}








