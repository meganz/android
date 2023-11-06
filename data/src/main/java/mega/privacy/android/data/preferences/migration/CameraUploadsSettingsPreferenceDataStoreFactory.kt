package mega.privacy.android.data.preferences.migration

import androidx.datastore.preferences.core.MutablePreferences
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.preferences.CameraUploadsSettingsPreferenceDataStore
import javax.inject.Inject

internal class CameraUploadsSettingsPreferenceDataStoreFactory @Inject constructor(
    private val encryptData: EncryptData,
    private val decryptData: DecryptData,
) {
    operator fun invoke(preferences: MutablePreferences) =
        CameraUploadsSettingsPreferenceDataStore(preferences, encryptData, decryptData)
}
