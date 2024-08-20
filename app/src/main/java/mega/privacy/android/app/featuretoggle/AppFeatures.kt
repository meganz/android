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
     * Enables document scanner
     */
    DocumentScanner(
        "Enable new document scanner implementation",
        false
    ),

    /**
     * Enables Map location
     */
    MapLocation(
        "Enable map location feature",
        false,
    ),

    /**
     * Enables new video player
     */
    NewVideoPlayer(
        "Enable new video player",
        false,
    ),

    /**
     * Enables report issue via email in login screen
     */
    ReportIssueViaEmail(
        "Enable report issue via email in login screen",
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
     * Enables new video section flag
     */
    NewVideoSection(
        "Enable new video section flag",
        true
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
     * Meeting notification settings
     */
    MeetingNotificationSettings("Enable Meeting notification settings", false),

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
     * Raise to speak in a call
     */
    RaiseToSpeak("Raise to speak in a call or a meeting", true),

    /**
     * Camera uploads utilizes active transfers to monitor transfers
     */
    ActiveTransfersInCameraUploads(
        "Camera Uploads uses Active transfers to monitor the transfer progress",
        true,
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
     * Enables new transfers section
     */
    TransfersSection(
        "Enable new transfers fragment",
        false
    ),

    /**
     * New import section
     */
    NewUploadDestinationActivity(
        "Enable upload destination activity",
        false
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue
    }
}
