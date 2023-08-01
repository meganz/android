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
     * Enable Camera Uploads Performance benchmark
     */
    CameraUploadsPerformance(
        "Enable Camera Uploads Performance benchmark",
        false
    ),

    /**
     * Enables the new compose passcode implementation
     */
    Passcode(
        "Enables the new compose passcode implementation",
        false,
    ),

    /**
     * Enables the options to cancel scheduled meeting and cancel an occurrence
     */
    CancelSchedMeeting(
        "Enables the options to cancel scheduled meeting and cancel an occurrence",
        false
    ),

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
        true
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
    FileBrowserCompose("Enable compose for FileBrowser", true),

    /**
     * Meeting notification settings
     */
    MeetingNotificationSettings("Enable Meeting notification settings", false),

    /**
     * File sharing contact verification
     */
    ContactVerification(
        "Enable new improved contact verification reminders",
        false,
    ),

    /**
     * To switch into new FileLink compose screen
     */
    FileLinkCompose("Enable compose for FileLink", false)

    ;

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            values().firstOrNull { it == feature }?.defaultValue
    }
}
