package mega.privacy.android.feature.sync.data.gateway

internal interface SyncPreferencesDatastore {

    suspend fun setOnboardingShown(shown: Boolean)

    suspend fun getOnboardingShown(): Boolean
}
