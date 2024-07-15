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
     * Enables new video player
     */
    NewVideoPlayer(
        "Enable new video player",
        false,
    ),

    /**
     * Enables new video queue
     */
    NewZipBrowser(
        "Enable new zip browser",
        true,
    ),

    /**
     * Enables new video queue
     */
    NewVideoQueue(
        "Enable new video queue",
        true,
    ),

    /**
     * Enables report issue via email in login screen
     */
    ReportIssueViaEmail(
        "Enable report issue via email in login screen",
        true,
    ),

    /**
     * Enables new audio queue
     */
    NewAudioQueue(
        "Enable new audio queue",
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
     * Enables the Hidden Nodes feature
     */
    HiddenNodes(
        "Enables the Hidden Nodes feature",
        false,
    ),

    /**
     * Shows the "Upload only while charging" Option in the refactored Settings Camera Uploads screen
     */
    SettingsCameraUploadsUploadWhileCharging(
        description = "Shows the Upload only while charging Option in the refactored Settings Camera " +
                "Uploads screen.",
        defaultValue = true,
    ),

    /**
     * Enables new video section flag
     */
    NewVideoSection(
        "Enable new video section flag",
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
    OfflineCompose("Enable compose for Offline Screen", true),

    /**
     * Meeting notification settings
     */
    MeetingNotificationSettings("Enable Meeting notification settings", false),

    /**
     * Search implementation with dropdown chips
     */
    DropdownChips("Search implementation with dropdown chips and bottom sheet", true),

    /**
     * To enable showing promo notifications in Notifications screen
     */
    PromoNotifications("Enable promotional notifications", true),

    /**
     * To enable the new add and manage description feature to node
     */
    NodeWithDescription(
        "Enable node with description",
        true
    ),

    /**
     * To enable search by node description
     */
    SearchWithDescription(
        "Enable search with description",
        false
    ),

    /**
     * To enable the new cancel subscription feature
     */
    CancelSubscription(
        "Enable cancel subscription feature",
        false
    ),

    /**
     * Enables new manage chat history compose page
     */
    NewManageChatHistoryActivity(
        "Enable new manage chat history activity",
        true
    ),

    /**
     * Raise to speak in a call
     */
    RaiseToSpeak("Raise to speak in a call or a meeting", false),

    /**
     * Enables UploadWorker for upload files, instead of UploadService. This is a work in progress feature.
     */
    UploadWorker(
        "Enables UploadWorker for download files, instead of UploadService. This is a work in progress feature.",
        true,
    ),

    /**
     * Camera uploads utilizes active transfers to monitor transfers
     */
    ActiveTransfersInCameraUploads(
        "Camera Uploads uses Active transfers to monitor the transfer progress",
        false,
    ),

    /**
     * Enables new confirm email fragment compose page
     */
    NewConfirmEmailFragment(
        "Enable new confirm email fragment",
        true
    ),

    /**
     * Enables new tour fragment compose page
     */
    NewTourFragment(
        "Enable new tour fragment",
        true
    ),

    /**
     * To enable the new add and manage description feature to node
     */
    NodeWithTags(
        "Enable node with tags",
        false
    ),

    /**
     * Enables Picture in Picture (Pip) in Meeting
     */
    PictureInPicture(
        "Enable Picture in Picture in Meeting",
        false
    ),

    /**
     * Enables new invite contact compose page
     */
    NewInviteContactActivity(
        "Enable new invite contact activity",
        true
    ),

    /**
     * Enables new transfers section
     */
    TransfersSection(
        "Enable new transfers fragment",
        false
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue
    }
}
