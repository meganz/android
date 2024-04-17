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
     * Enables new audio queue
     */
    NewAudioQueue(
        "Enable new audio queue",
        false,
    ),

    /**
     * Enables recent actions compose tab
     */
    RecentActionsCompose(
        "Enables new Recent Actions Compose tab (requires app restart)",
        true,
    ),

    /**
     * Enables the report issue button on the login screen
     */
    LoginReportIssueButton(
        "Enables the report issue button on the login screen feature",
        true,
    ),

    /**
     * Enables new document section flag
     */
    NewDocumentSection(
        "Enable new document section flag",
        true,
    ),

    /**
     * Enables the Hidden Nodes feature
     */
    HiddenNodes(
        "Enables the Hidden Nodes feature",
        false,
    ),

    /**
     * Enables the Settings Camera Uploads page in Jetpack Compose
     */
    SettingsCameraUploadsCompose(
        "Enables the Settings Camera Uploads in Jetpack Compose. This requires an app " +
                "restart for the changes to take effect.",
        true,
    ),


    /**
     * Enables new incoming shares compose page
     */
    IncomingSharesCompose(
        "Enable new Incoming Shares Compose page (requires app restart)",
        true
    ),


    /**
     * Enables new outgoing shares compose page
     */
    OutgoingSharesCompose(
        "Enable new Outgoing Shares Compose page (requires app restart)",
        true
    ),


    /**
     * Enables new links compose page
     */
    LinksCompose(
        "Enable new Shared Links Compose page (requires app restart)",
        true
    ),

    /**
     * Enables new video section flag
     */
    NewVideoSection(
        "Enable new video section flag",
        false
    ),

    /**
     * Enables new Image Preview flag
     */
    ImagePreview(
        "Enables new Image Preview flag",
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
        true,
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
     * Permanent logging toggle
     */
    PermanentLogging("Permanently enable logging, removing functionality to turn it on/off", true),

    /**
     * App Test toggle
     */
    AppTest("This is a test toggle. It does nothing", false),

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
    SearchWithChips("Advanced search implementation in clean architecture", true),

    /**
     * Search implementation with dropdown chips
     */
    DropdownChips("Search implementation with dropdown chips and bottom sheet", true),

    /**
     * To enable showing promo notifications in Notifications screen
     */
    PromoNotifications("Enable promotional notifications", true),

    /**
     * To switch into new chat activity
     * Enabled version 11.10
     */
    NewChatActivity("Enable new chat activity", true),

    /**
     * Call unlimited for pro users
     */
    CallUnlimitedProPlan("Call to stay unlimited when host with pro plan leaves", false),

    /**
     * To show strings for new features (meetings and VPN), strings should be hidden until the features are released
     */
    ShowStringsForNewFeatures(
        "Show strings for new features (meetings and VPN)",
        false
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue
    }
}
