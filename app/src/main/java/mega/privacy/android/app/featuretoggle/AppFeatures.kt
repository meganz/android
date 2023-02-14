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
     * Monitor phone number
     */
    MonitorPhoneNumber(
        "Switch to using the new notification mechanism for monitoring the verified phone number. Enabling this before implementation is complete might result in updates not being received by all screens",
        false
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
     * Indicates if the user is cryptographically secure
     */
    MandatoryFingerprintVerification(
        "Indicates if mandatory fingerprint verification needs to be done",
        false
    ),

    /**
     * User albums toggle
     */
    UserAlbums("Enable user albums feature", false),

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
    AppTest("This is a test toggle. It does nothing", false);


    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            values().firstOrNull { it == feature }?.defaultValue
    }
}