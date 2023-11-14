package mega.privacy.android.data.preferences.migration

import androidx.datastore.preferences.core.MutablePreferences
import mega.privacy.android.data.cryptography.DecryptData2
import mega.privacy.android.data.cryptography.EncryptData2
import mega.privacy.android.data.preferences.CameraUploadsSettingsPreferenceDataStore
import javax.inject.Inject

internal class CameraUploadsSettingsPreferenceDataStoreFactory @Inject constructor(
    private val encryptData: EncryptData2,
    private val decryptData: DecryptData2,
) {
    operator fun invoke(preferences: MutablePreferences) =
        CameraUploadsSettingsPreferenceDataStore(preferences, encryptData, decryptData)
}
