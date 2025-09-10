package mega.privacy.android.app.features

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority

internal enum class CloudDriveFeature(
    override val description: kotlin.String,
    private val defaultValue: Boolean,
) : Feature {
    /**
     * Toggle for enabling favorite multiple selection in Cloud Drive.
     */
    FAVORITE_MULTIPLE_SELECTION(
        "Allow multiple selection for favorite in Cloud Drive (SAO-2344)",
        true
    ),

    /**
     * Toggle for enabling label multiple selection in Cloud Drive.
     */
    LABEL_MULTIPLE_SELECTION(
        "Allow multiple selection for labeling in Cloud Drive (SAO-3057)",
        true
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            CloudDriveFeature.entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}
