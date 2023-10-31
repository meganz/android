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
     * Enables new Image Preview flag
     */
    ImagePreview(
        "Enables new Image Preview flag",
        false,
    ),

    /**
     * Enables revamp CU interface
     */
    NewCU(
        "Enables revamp CU interface",
        false,
    ),

    /**
     * Enables subfolder media discovery setting
     */
    SubFolderMediaDiscoverySetting(
        "Enables subfolder media discovery  setting",
        false,
    ),

    /**
     * Enables new media discovery fab
     */
    NewMediaDiscoveryFab(
        "Enables new media discovery fab",
        false,
    ),

    /**
     * Enables new architecture of album.
     * Remove once regression testing completed
     */
    ReworkAlbum(
        "Enables new architecture of album",
        false,
    ),

    /**
     * Enables prefetch timeline photos as soon initialization screen completed
     */
    PrefetchTimeline(
        "Enables prefetch timeline photos as soon initialization screen completed",
        false,
    ),

    /**
     * Enables the new compose passcode backend implementation
     */
    PasscodeBackend(
        "Enables the new compose passcode backend implementation",
        true,
    ),

    /**
     * Enables the new compose passcode implementation
     */
    Passcode(
        "Enables the new compose passcode implementation",
        true,
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
        true
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
     * App Test toggle
     */
    AppTest("This is a test toggle. It does nothing", false),

    /**
     * To switch into new Plans page UI
     */
    PlansPageUpdate("Enable new design for Upgrade account view", false),

    /**
     * To switch into new Offline Screen Compose UI
     */
    OfflineCompose("Enable compose for Offline Screen", false),

    /**
     * Meeting notification settings
     */
    MeetingNotificationSettings("Enable Meeting notification settings", false),

    /**
     * Clean refactored search implementation
     */
    SearchWithChips("Advanced search implementation in clean architecture", false),

    /**
     * In-App Ads toggle
     */
    InAppAdvertisement("Enable In-App Ads", false),

    /**
     * To switch into new QRCode compose screen with Google code scanner
     */
    QRCodeCompose("Enable compose for QRCode with google code scanner", true),

    /**
     * To switch into new chat activity
     */
    NewChatActivity("Enable new chat activity", false)
    ;

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            values().firstOrNull { it == feature }?.defaultValue
    }
}
