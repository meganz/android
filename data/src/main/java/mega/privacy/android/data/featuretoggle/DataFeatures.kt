package mega.privacy.android.data.featuretoggle

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

/**
 * Data features
 *
 * @property description
 * @property defaultValue
 *
 * Note: Please register your feature flag to the top of the list to minimize git-diff changes.
 */
enum class DataFeatures(
    override val description: String,
    private val defaultValue: Boolean,
) : Feature {
    /**
     * Enable camera uploads new notification
     */
    CameraUploadsNotification(
        "Enable camera uploads new notification",
        false,
    ),

    /**
     * Use new camera uploads records
     */
    UseCameraUploadsRecords(
        "Use Camera Uploads Records",
        true
    ),

    /**
     * Show a single download finish notifications for each user action
     */
    ShowGroupedDownloadNotifications(
        "Show a single download finish notifications for each user action",
        true,
    ),

    /**
     * Show a single upload progress and finish notifications for each user action
     */
    ShowGroupedUploadNotifications(
        "Show a single upload progress and finish notifications for each user action",
        true,
    ),
    ;

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue

        override val priority = FeatureFlagValuePriority.Default
    }
}
