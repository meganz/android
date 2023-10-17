package mega.privacy.android.data.preferences.security

import androidx.datastore.preferences.core.MutablePreferences
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import javax.inject.Inject

internal class PasscodeDataStoreFactory @Inject constructor(
    private val encryptData: EncryptData,
    private val decryptData: DecryptData,
) {
    operator fun invoke(preferences: MutablePreferences) =
        PasscodeDataStore(preferences, encryptData, decryptData)
}