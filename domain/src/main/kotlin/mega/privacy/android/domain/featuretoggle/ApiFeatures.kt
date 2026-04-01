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
 */
enum class ApiFeatures(
    override val experimentName: String,
    override val description: String,
    private val defaultValue: Boolean,
    override val checkRemote: Boolean = true,
    override val singleCheckPerRun: Boolean = false,
) : ApiFeature {
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
     * Flag to allow multiple selection for favorite/label in Cloud Drive.
     * When enabled, users can select multiple favorites or labels at once.
     *
     * Default: false
     */
    AllowMultipleSelectionsEnabled(
        experimentName = "mult1",
        description = "Allow multiple selection for favorite/label in Cloud Drive",
        defaultValue = false
    ),

    /**
     * Media Revamp phase 2 feature flag
     */
    MediaRevampPhase2(
        experimentName = "mrp2",
        description = "Enable Media Revamp phase 2 features",
        defaultValue = false
    ),

    /**
     * Text editor Compose migration (AND-22552).
     * When enabled, opens the text editor in the Compose screen (Nav3 + Material 3) instead of the legacy Activity.
     */
    TextEditorCompose(
        experimentName = "andte",
        description = "Convert the text editor to Compose (Nav3 + Material 3)",
        singleCheckPerRun = true,
        defaultValue = false
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
     * Enable Audios chip in Home screen
     */
    AudiosChipInHome(
        experimentName = "acih",
        description = "Enable Audios chip in Home screen",
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
     * Audio Section Revamp feature flag (AND-22975).
     * Callers use `AudioSectionNavKey`; the app host gates Compose (`AudioNavKey`) vs legacy activity.
     */
    AudioSectionRevamp(
        experimentName = "asr1",
        description = "Navigate to the new Compose-based Audio section instead of the legacy one",
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
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}
