package mega.privacy.android.app.domain.usecase

interface SetFeatureFlag {

    suspend operator fun invoke(featureName: String, isEnabled: Boolean)
}