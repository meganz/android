package mega.privacy.android.app.domain.usecase

/**
 * Sets feature flag value
 */
interface SetFeatureFlag {

    /**
     * Invoke.
     * @param featureName : Name of the feature
     * @param isEnabled : Boolean value
     */
    suspend operator fun invoke(featureName: String, isEnabled: Boolean)
}