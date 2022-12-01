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
     * Indicates if the user is cryptographically secure
     */
    Secure("Indicates if user is cryptographically secure", false),

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
     * Dynamic album layout toggle
     */
    DynamicAlbum("Enable dynamic album feature", false),

    /**
     * App Test toggle
     */
    AppTest("This is a test toggle. It does nothing", false);


    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            values().firstOrNull { it == feature }?.defaultValue
    }
}