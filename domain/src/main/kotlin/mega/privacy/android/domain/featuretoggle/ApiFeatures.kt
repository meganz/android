package mega.privacy.android.domain.featuretoggle

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ApiFeature

/**
 * Remote Api features
 *
 * @property experimentName Name of the AB test flag which we get from aPI team
 * @property description
 * @property defaultValue
 * @property checkRemote If true, the value will be checked from the remote server, if set to false we can toggle the flag as usual feature flag from the Settings in QA build
 * @property singleCheckPerRun If true, the remote value is fetched once based on the account
 * state at app startup, cached for the entire lifetime of the app process
 */
enum class ApiFeatures(
    override val experimentName: String,
    override val description: String,
    private val defaultValue: Boolean,
    override val checkRemote: Boolean = true,
    override val singleCheckPerRun: Boolean = false,
) : ApiFeature {

    /**
     * Quota-warning upsell screen (AND-24264).
     * When enabled, the storage over-quota flow shows the new quota-warning upsell screen instead
     * of the legacy over-quota dialog.
     */
    QuotaWarningUpsellScreen(
        experimentName = "qwup",
        description = "Show the quota-warning upsell screen instead of the over-quota dialog",
        defaultValue = false,
    ),

    /**
     * Enables file info revamp
     */
    FileInfoRevamp(
        "aflin",
        "Enable file info revamp",
        false,
    ),

    /**
     * Enable video player revamp public link
     */
    VideoPlayerRevampPublicLink(
        experimentName = "vprpl",
        description = "Enable revamped video player for public link access when not logged in",
        defaultValue = false,
        singleCheckPerRun = true,
    ),

    /**
     * Enables video editor
     */
    VideoEditor(
        "vdedt",
        "Enable video editor",
        false,
    ),

    /**
     * Enables Picture in Picture (PIP) in Video Player
     */
    VideoPlayerPictureInPicture(
        "vppip",
        "Enable Picture in Picture in Video Player",
        false,
    ),

    /**
     * File link revamp
     */
    FileLinkRevamp(
        "aflnk",
        "Enable file link revamp screen",
        false,
    ),

    /**
     * Share & Manage link revamp
     */
    ShareLinkRevamp(
        "slink",
        "Enable the Share & Manage link revamp screens",
        false,
    ),

    /**
     * Enabled rewarded ads in public link screens. AND-21960
     */
    RewardedAds(
        "grads",
        "Enable rewarded ads in public links screen",
        false
    ),

    /**
     * Continuous document scanner (AND-22951).
     * When enabled, uses the custom continuous document scanner with real-time
     * boundary detection, auto-capture, and page-flip detection.
     * When disabled, the existing ML Kit Document Scanner is used.
     */
    ContinuousDocumentScanner(
        experimentName = "cscan",
        description = "Enable custom continuous document scanner",
        singleCheckPerRun = true,
        defaultValue = false
    ),

    /**
     * Call unlimited for pro users
     */
    CallUnlimitedProPlan(
        "chmon",
        "Call to stay unlimited when host with pro plan leaves",
        false
    ),

    /**
     * Enable Google ads with feature flag "ff_adse" or A/B test flag "ab_adse"
     */
    GoogleAdsFeatureFlag(
        "adse",
        "Enable Google Ads",
        false
    ),

    /**
     * Migration to mega app domain.
     */
    MegaDotAppDomain(
        "site",
        "Enable migration to mega app domain",
        false
    ),

    /**
     * Age Signal Check feature flag
     *
     * Controls whether the app checks the user's age signal to hide Stripe payment method
     * if the user is under the allowed age. When enabled, Stripe as a payment option
     * is not shown to users under age according to Google's Age Signals API.
     *
     * Default: false
     */
    AgeSignalsCheckEnabled(
        experimentName = "ages1",
        description = "Do not show Stripe payment method if it is under age",
        defaultValue = false
    ),

    /**
     * PDF Viewer Compose UI.
     * When enabled, PDF files are displayed using the new Compose-based
     * PdfViewerScreen. When disabled, the legacy PdfViewerActivity is used.
     */
    PdfViewerComposeUI(
        experimentName = "pdfs",
        description = "Enable PDF Viewer with ComposeUI and single activity",
        singleCheckPerRun = true,
        defaultValue = true
    ),

    /**
     * Enable DCIM folder to be selected as Sync/Backup and detect cross device Sync/CU cloud folder conflicts
     */
    DCIMSelectionAsSyncBackup(
        experimentName = "dcims",
        description = "Enable DCIM folder to be selected as Sync/Backup and detect cross device Sync/CU cloud folder conflicts",
        defaultValue = false
    ),

    /**
     * Restrict syncing the same cloud folder across different devices.
     *
     * When disabled (default):
     * - Sync folder selection excludes Sync/Backup from OTHER devices (allows Sync-Sync across devices)
     * - Camera Uploads folder selection still checks Sync/Backup from ALL devices (blocks CU-Sync)
     *
     * When enabled:
     * - All folder selections check Sync/Backup from ALL devices
     *
     * Default: false (sync across devices is allowed by default)
     */
    RestrictSyncAcrossDevices(
        experimentName = "dsad",
        description = "Restrict syncing the same cloud folder across different devices",
        singleCheckPerRun = true,
        defaultValue = false
    ),

    /**
     * Enable Whats New feature dialog
     */
    WhatsNewFeatureDialog(
        experimentName = "wnfd",
        description = "Enable Whats New feature dialog",
        defaultValue = false
    ),

    /**
     * Continue where you left off feature (AND-23051).
     * When enabled, persists user progress (PDF page, video/audio position, text editor cursor)
     * and shows a Home screen carousel widget for quick resume.
     */
    ContinueWhereLeftOff(
        experimentName = "acwlo",
        description = "Enable continue where you left off feature",
        defaultValue = false
    ),

    /**
     * Video Player Revamp feature flag.
     * When enabled, opens the revamped Video Player (VideoPlayerRevampActivity) instead of the legacy one.
     * singleCheckPerRun = true to cache the flag value for the lifetime of the app session and
     * prevent intermittent false returns caused by repeated remote fetches.
     */
    VideoPlayerRevamp(
        experimentName = "vprv",
        description = "Open the revamped Video Player instead of the legacy one",
        defaultValue = false,
        singleCheckPerRun = true,
    ),

    /**
     * Audio Player Revamp feature flag.
     * When enabled, opens the revamped Audio Player (AudioPlayerActivityV2) instead of the legacy one.
     */
    AudioPlayerRevamp(
        experimentName = "aprv",
        description = "Open the revamped Audio Player instead of the legacy one",
        defaultValue = false
    ),

    /**
     * Viewed links feature flag for Home Revamp Phase 2
     * Enable viewed links section on Home Screen
     */
    ViewedLinks(
        experimentName = "hrvl",
        description = "Enable viewed links section on Home Screen (Home Revamp Phase 2)",
        defaultValue = false
    ),

    /**
     * Home configuration feature flag for Home Revamp Phase 2
     * Enable home configuration to reorder widgets
     */
    HomeConfiguration(
        experimentName = "hconf",
        description = "Home configuration and reordering of home widgets",
        defaultValue = false
    ),

    /**
     * Google Sign-In on the login screen (AND-23415).
     * When enabled, shows a "Sign in with Google" button on the login screen.
     * Resolved pre-login via getMiscFlags() (unauthenticated SDK call).
     */
    GoogleSignIn(
        experimentName = "gsign",
        description = "Enable Google Sign-In on login screen",
        singleCheckPerRun = true,
        defaultValue = false
    ),

    CloudDriveDocumentProvider(
        experimentName = "cdsp",
        description = "Enable MEGA Cloud Drive as a SAF root in the Android system file picker",
        singleCheckPerRun = false,
        defaultValue = false,
    ),

    /**
     * Display MEGA core features in Home Screen
     */
    DoMoreWithMEGA(
        experimentName = "dmwm",
        description = "Promotes MEGA’s core features and encourages adoption through progressive onboarding",
        defaultValue = false
    ),

    /**
     * Feature flag to control the migration of MyAccountUsageFragment to ComposeUI.
     */
    MyAccountUsageFragmentComposeUI(
        experimentName = "myusg",
        description = "Enable ComposeUI MyAccountUsageFragment",
        singleCheckPerRun = true,
        defaultValue = false,
    ),

    /**
     * Render Markdown (.md/.markdown) files in the Compose text editor (AND-24001).
     * When enabled, Markdown files open in a formatted read-only preview in View mode;
     * Edit shows the raw source. When disabled, Markdown files behave as plain text.
     */
    TextEditorMarkdownRendering(
        experimentName = "temd",
        description = "Render Markdown files in the text editor as a formatted preview",
        singleCheckPerRun = true,
        defaultValue = false,
    ),

    /**
     * Subscription page discount design revamp (AND-23922 / DSN-3131).
     * When enabled, the upgrade screen shows the new "no-offer" design: a "Why go Pro?" card,
     * a current plan card with renewal/expiry date, a billing-period segmented control, and
     * per-plan buy buttons. When disabled, the existing upgrade screen is shown.
     */
    SubscriptionDiscountRevamp(
        experimentName = "subrv",
        description = "New subscription page design",
        defaultValue = false,
    ),

    /**
     * Sorting and view mode settings.
     * Enables the "Sorting and view mode" settings screen and its per-folder behaviour.
     */
    SortingAndViewMode(
        experimentName = "svms",
        description = "Enable the Sorting and view mode settings",
        defaultValue = false,
    ),

    /**
     * Flag to enable media timeline pagination with fast scroller support
     */
    MediaTimelinePagination(
        experimentName = "amtp",
        description = "Enable media timeline pagination with fast scroller",
        defaultValue = false,
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}
