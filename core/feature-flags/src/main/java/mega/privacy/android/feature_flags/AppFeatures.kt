package mega.privacy.android.feature_flags

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider


/**
 * App features
 *
 * @property description
 * @property defaultValue
 *
 * Note: Please register your feature flag to the top of the list to minimize git-diff changes.
 */
enum class AppFeatures(override val description: String, private val defaultValue: Boolean) :
    Feature {

    /**
     * Single activity
     */
    SingleActivity(
        "Enable single activity rewrite",
        false,
    ),

    /**
     * Search revamp
     */
    SearchRevamp(
        "Enable search revamp in phase-2 single activity",
        false,
    ),

    CameraUploadsPausedWarningBanner(
        "Enable Camera Uploads paused warning banner",
        true
    ),

    CameraUploadsTransferScreen(
        "Enable Camera Uploads transfer screen",
        true
    ),

    /**
     * UI-driven photo monitoring lifecycle
     */
    UIDrivenPhotoMonitoring(
        "Enable UI-driven photo monitoring lifecycle to prevent race conditions",
        true
    ),

    /**
     * Contacts compose u i
     */
    ContactsComposeUI(
        "Enable compose version of the contacts ui",
        false,
    ),

    /**
     * Enable compose implementation of the main settings screen
     */
    SettingsComposeUI(
        "Enable compose implementation of the main settings screen",
        false,
    ),

    /**
     * New contact request screen
     */
    NewContactRequestScreen(
        "Enable new contact request screen",
        false
    ),

    /**
     * Enables prefetch timeline photos as soon initialization screen completed
     */
    PrefetchTimeline(
        "Enables prefetch timeline photos as soon initialization screen completed",
        false,
    ),

    /**
     * App Test toggle
     */
    AppTest("This is a test toggle. It does nothing", false),

    /**
     * Enables Picture in Picture (Pip) in Meeting
     */
    PictureInPicture(
        "Enable Picture in Picture in Meeting",
        false
    ),

    /**
     * New import section
     */
    NewUploadDestinationActivity(
        "Enable upload destination activity",
        false
    ),

    /**
     * Call settings implemented with the new components library
     */
    CallSettingsNewComponents(
        "Call settings implemented with the new components library",
        false,
    ),

    /**
     *  Enable pagination in timeline photos
     */
    TimelinePhotosPagination(
        "Enable pagination in timeline photos",
        false,
    ),

    /**
     * Feature flag to control the migration of NodeLabelBottomSheetDialogFragment to Kotlin.
     */
    NodeLabelBottomSheetDialogFragmentConversion(
        "Convert the NodeLabelBottomSheetDialogFragment to Kotlin",
        true
    ),

    /**
     * Feature flag to control the migration of MyAccountUsageFragment to ComposeUI.
     */
    MyAccountUsageFragmentComposeUI(
        "Convert the MyAccountUsageFragment to ComposeUI",
        false
    ),
    ;

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}
