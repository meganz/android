package mega.privacy.android.data.mapper.login

import androidx.datastore.preferences.core.Preferences
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.preferences.EphemeralCredentialsDataStore
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import javax.inject.Inject

internal class EphemeralCredentialsMapper @Inject constructor(
    private val decryptData: DecryptData,
) {
    suspend operator fun invoke(preferences: Preferences): EphemeralCredentials? {
        val session =
            decryptData(preferences[EphemeralCredentialsDataStore.sessionPreferenceKey]).orEmpty()
        if (session.isBlank()) return null
        return EphemeralCredentials(
            email = decryptData(preferences[EphemeralCredentialsDataStore.emailPreferenceKey]).orEmpty(),
            password = decryptData(preferences[EphemeralCredentialsDataStore.passwordPreferenceKey]).orEmpty(),
            session = decryptData(preferences[EphemeralCredentialsDataStore.sessionPreferenceKey]).orEmpty(),
            firstName = decryptData(preferences[EphemeralCredentialsDataStore.firstNamePreferenceKey]).orEmpty(),
            lastName = decryptData(preferences[EphemeralCredentialsDataStore.lastNamePreferenceKey]).orEmpty(),
        )
    }
}