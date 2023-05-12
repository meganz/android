package mega.privacy.android.data.mapper.login

import androidx.datastore.preferences.core.MutablePreferences
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.preferences.EphemeralCredentialsDataStore
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import javax.inject.Inject

internal class EphemeralCredentialsPreferenceMapper @Inject constructor(
    private val encryptData: EncryptData,
) {
    suspend operator fun invoke(preferences: MutablePreferences, ephemeral: EphemeralCredentials) {
        preferences[EphemeralCredentialsDataStore.emailPreferenceKey] =
            encryptData(ephemeral.email).orEmpty()
        preferences[EphemeralCredentialsDataStore.passwordPreferenceKey] =
            encryptData(ephemeral.password).orEmpty()
        preferences[EphemeralCredentialsDataStore.sessionPreferenceKey] =
            encryptData(ephemeral.session).orEmpty()
        preferences[EphemeralCredentialsDataStore.firstNamePreferenceKey] =
            encryptData(ephemeral.firstName).orEmpty()
        preferences[EphemeralCredentialsDataStore.lastNamePreferenceKey] =
            encryptData(ephemeral.lastName).orEmpty()
    }
}