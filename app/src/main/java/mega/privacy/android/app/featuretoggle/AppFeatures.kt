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

    CameraActivityInCloudDrive(
        "Enable Camera Activity in Cloud Drive to capture photos and videos",
        true,
    ),

    /**
     * Single activity
     */
    SingleActivity(
        "Enable single activity rewrite",
        false,
    ),

    /**
     * Achievements Free Trial Reward
     */
    AchievementsFreeTrialReward(
        "Enable achievements free trial reward",
        true,
    ),

    /**
     * File contacts compose u i
     */
    FileContactsComposeUI(
        "Enable compose version of the file contacts ui",
        false,
    ),

    /**
     * Contacts compose u i
     */
    ContactsComposeUI(
        "Enable compose version of the contacts ui",
        false,
    ),

    /**
     * New psa state
     */
    NewPsaState(
        "Use new psa state in stead of legacy psa state singleton. Legacy psa state exists to unify behaviour while legacy screens still exist",
        false,
    ),

    /**
     * Enable compose implementation of the main settings screen
     */
    SettingsComposeUI(
        "Enable compose implementation of the main settings screen",
        false,
    ),

    /**
     * New contact request screen
     */
    NewContactRequestScreen(
        "Enable new contact request screen",
        false
    ),

    /**
     * Enables Map location
     */
    MapLocation(
        "Enable map location feature",
        true,
    ),

    /**
     * Enables new video player
     */
    NewVideoPlayer(
        "Enable new video player",
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
     * Shares compose
     */
    SharesCompose(
        "Enable compose implementation of shares tabs",
        false
    ),

    /**
     * App Test toggle
     */
    AppTest("This is a test toggle. It does nothing", false),

    /**
     * To enable search by node description
     */
    SearchWithDescription(
        "Enable search with description",
        true
    ),

    /**
     * To enable search by node tags
     */
    SearchWithTags(
        "Enable search with tags",
        true
    ),

    /**
     * To enable the new add and manage description feature to node
     */
    NodeWithTags(
        "Enable node with tags",
        true
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
     *  Login Revamp with new components
     */
    LoginRevamp(
        "Login Revamp with new components",
        true,
    ),

    /**
     *  Registration Revamp with new components
     */
    RegistrationRevamp(
        "Registration Revamp with new components",
        true,
    ),

    /**
     *  Onboarding Revamp with new components
     */
    OnboardingRevamp(
        "Onboarding Revamp with new components",
        true,
    ),

    /**
     *  Onboarding Pro Promo Revamp with new components
     */
    OnboardingProPromoRevamp(
        "Onboarding Pro Promo Revamp with new components",
        true,
    ),

    /**
     *  Update Account Revamp with new components
     */
    UpdateAccountRevamp(
        "Update Account Revamp with new components",
        false,
    ),

    /**
     *  Enable pagination in timeline photos
     */
    TimelinePhotosPagination(
        "Enable pagination in timeline photos",
        false,
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}
