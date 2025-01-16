package mega.privacy.android.app.featuretoggle

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
     * Enable the menu of Favourites playlist
     */
    FavouritesPlaylistMenuEnabled(
        "Enable Favourites playlist menu",
        true
    ),

    /**
     * Enable compose implementation of the main settings screen
     */
    SettingsComposeUI(
        "Enable compose implementation of the main settings screen",
        false,
    ),

    /**
     * Enable the video player zoom in feature
     */
    VideoPlayerZoomInEnable(
        "Enable add video player zoom in",
        true
    ),

    /**
     * Add video to playlist from video section
     */
    AddVideoToPlaylistFromVideoSection(
        "Enable add video to playlist from video section",
        true
    ),

    /**
     * New contact request screen
     */
    NewContactRequestScreen(
        "Enable new contact request screen",
        false
    ),

    /**
     * New create account fragment(compose version)
     */
    NewCreateAccountFragment(
        "Enable new create account fragment",
        true
    ),

    /**
     * Enable request status progress dialog
     */
    RequestStatusProgressDialog(
        "Enable request status progress dialog",
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
        true,
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
     *  Cloud Drive and Sync in a single screen with the Two Tabs (Cloud Drive and Syncs)
     */
    CloudDriveAndSyncs(
        "Implement Cloud Drive and Sync in a single screen with the Two Tabs (Cloud Drive and Syncs)",
        false,
    ),
    ;

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}
