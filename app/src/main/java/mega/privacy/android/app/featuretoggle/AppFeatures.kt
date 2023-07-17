package mega.privacy.android.app.featuretoggle

import mega.privacy.android.domain.entity.Feature
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
     * Enables DownloadWorker for download nodes, instead of DownloadService. This is a work in progress feature.
     */
    DownloadWorker(
        "Enables DownloadWorker for download nodes, instead of DownloadService. This is a work in progress feature.",
        false,
    ),

    /**
     * Enables the Device Center functionality by displaying "Device center" in the Dashboard
     * Side Menu
     */
    DeviceCenter(
        "Enables the Device Center in the Dashboard Side Menu. The Device Center shows " +
                "the list of Devices that have uploaded content through Camera Uploads or Backups.",
        false,
    ),

    /**
     * Enable the remember timeline preferences feature
     */
    RememberTimelinePreferences(
        "Remember the Timeline filter preferences",
        true,
    ),

    /**
     * To switch to Two Factor Authentication compose
     */
    TwoFactorAuthenticationCompose(
        "Enable compose implementation of Two Factor Authentication screen",
        false
    ),

    /**
     * Shares compose
     */
    SharesCompose(
        "Enable compose implementation of shares tabs",
        false
    ),

    /**
     * Enable album sharing feature.
     */
    AlbumSharing(
        "Enable album sharing feature",
        false
    ),

    /**
     * To switch into new compose slideshow or not toggle.
     */
    SlideShowCompose(
        "Enable slideshow compose",
        true
    ),

    /**
     * To switch into new photos modularization architecture or not toggle
     */
    PhotosCompose(
        "Enable compose-world photos feature (modularization)",
        false
    ),

    /**
     * Android Sync toggle
     */
    AndroidSync(
        "Enable a synchronization between folders on local storage and folders on MEGA cloud",
        false
    ),


    /**
     * Sets the MegaApi::setSecureFlag
     */
    SetSecureFlag("Sets the secure flag value for MegaApi", false),

    /**
     * User albums toggle
     */
    UserAlbums("Enable user albums feature", true),

    /**
     * Permanent logging toggle
     */
    PermanentLogging("Permanently enable logging, removing functionality to turn it on/off", false),

    /**
     * Schedule Meeting toggle
     */
    ScheduleMeeting("Enable schedule meetings feature", false),

    /**
     * App Test toggle
     */
    AppTest("This is a test toggle. It does nothing", false),

    /**
     * To switch into new Plans page UI
     */
    PlansPageUpdate("Enable new design for Upgrade account view", false),

    /**
     * To switch into new FileBrowser Compose UI
     */
    FileBrowserCompose("Enable compose for FileBrowser", false),

    /**
     * Meeting notification settings
     */
    MeetingNotificationSettings("Enable Meeting notification settings", false),

    /**
     * To use remote feature flag 'ab_devtest' from API
     * this flag can be used to test the SDK methods getABTestValue() and sendABTestActive()
     * this flag can be used to test any A/B testing implementation before the API flag for real campaign will be created
     * this flag is not part of any existing A/B testing campaign
     * for real A/B testing campaign new flag will be created on API side and new enum type should be created here
     */
    devtest(
        "Remote feature flag from API for any tests related to A/B testing",
        false
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            values().firstOrNull { it == feature }?.defaultValue
    }
}