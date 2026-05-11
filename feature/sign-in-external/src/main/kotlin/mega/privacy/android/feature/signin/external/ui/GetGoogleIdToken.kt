package mega.privacy.android.feature.signin.external.ui

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import mega.privacy.android.domain.exception.login.GoogleSignInException
import mega.privacy.android.feature.signin.external.BuildConfig

/**
 * Launches Google Sign-In via Credential Manager and returns the ID token.
 *
 * Requires an [Activity] receiver because Credential Manager needs an activity
 * context to host its credential picker UI.
 *
 * @return The raw Google ID token JWT string.
 * @throws [GoogleSignInException] on failure.
 */
internal suspend fun Activity.getGoogleIdToken(): String {
    val credentialManager = CredentialManager.create(this)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
        .setFilterByAuthorizedAccounts(false)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return runCatching {
        val result = credentialManager.getCredential(this, request)
        GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    }.getOrElse { e ->
        throw when (e) {
            is GetCredentialCancellationException -> GoogleSignInException.Cancelled
            is NoCredentialException -> GoogleSignInException.NoCredential
            else -> GoogleSignInException.Unknown(e.message)
        }
    }
}
