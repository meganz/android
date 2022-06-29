package mega.privacy.android.app.domain.usecase

/**
 * Use case to set Feature Flag
 */
fun interface SetFeatureFlag {

    /**
     * Sets value of feature flag
     *
     * @param featureName: Name of the feature
     * @param isEnabled: Boolean value
     */
    suspend operator fun invoke(featureName: String, isEnabled: Boolean)
}