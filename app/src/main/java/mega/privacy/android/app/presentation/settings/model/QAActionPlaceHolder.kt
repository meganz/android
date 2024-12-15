package mega.privacy.android.app.presentation.settings.model

fun interface SetFeatureFlagPlaceHolder{
    suspend operator fun invoke(feature: String, enabled: Boolean)
}