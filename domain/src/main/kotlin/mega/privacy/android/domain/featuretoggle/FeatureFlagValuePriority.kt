package mega.privacy.android.domain.featuretoggle

/**
 * Feature flag value priority
 *
 * Defines the priority of the [FeatureFlagValueProvider] values.
 * The higher the priority providers will override the values provided by lower priority providers.
 * The order of priority from lowest to highest is:
 *      Default
 *      ConfigurationFile
 *      BuildTimeOverride
 *      RemoteToggled
 *      Cached
 *      RuntimeOverride
 *
 * This order is based on the ordinal of the enum value
 */
enum class FeatureFlagValuePriority {
    /**
     * Default
     */
    Default,

    /**
     * Configuration file
     */
    ConfigurationFile,

    /**
     * Build time override
     */
    BuildTimeOverride,

    /**
     * Remote toggled
     */
    RemoteToggled,

    /**
     * Cached
     */
    Cached,

    /**
     * Runtime override
     */
    RuntimeOverride;
}