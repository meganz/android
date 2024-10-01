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
     * Enable video recently watched feature
     */
    VideoRecentlyWatched(
        "Enable video recently watched feature",
        true
    ),

    /**
     * Compose passcode settings
     */
    ComposePasscodeSettings(
        "Use the new compose version of the passcode settings screens",
        true
    ),

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
     * To enable search by node tags
     */
    SearchWithTags(
        "Enable search with tags",
        false
    ),

    /**
     * To enable the new cancel subscription feature
     */
    CancelSubscription(
        "Enable cancel subscription feature",
        true
    ),

    /**
     * Raise to speak in a call
     */
    RaiseToSpeak("Raise to speak in a call or a meeting", true),

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
     * Enables full storage over quota banner
     */
    FullStorageOverQuotaBanner(
        "Enable full storage over quota banner",
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
