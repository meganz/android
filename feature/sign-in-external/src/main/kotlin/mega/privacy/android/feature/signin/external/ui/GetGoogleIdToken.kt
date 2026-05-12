package mega.privacy.android.feature.signin.external.ui

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import mega.privacy.android.domain.exception.login.GoogleSignInException
import mega.privacy.android.feature.signin.external.BuildConfig
import timber.log.Timber

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
    Timber.d("[GSIGN] Requesting Google ID token via Credential Manager")
    val credentialManager = CredentialManager.create(this)

    val signInOption = GetSignInWithGoogleOption
        .Builder(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    return runCatching {
        val result = credentialManager.getCredential(this, request)
        val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        Timber.d("[GSIGN] Credential Manager returned ID token (length=${idToken.length})")
        idToken
    }.getOrElse { e ->
        if (e is GetCredentialCancellationException) {
            Timber.d("[GSIGN] Credential Manager cancelled by user (${e.message})")
        } else {
            Timber.e(e, "[GSIGN] Credential Manager getCredential failed: ${e::class.simpleName}")
        }
        throw when (e) {
            is GetCredentialCancellationException -> GoogleSignInException.Cancelled
            is NoCredentialException -> GoogleSignInException.NoCredential
            else -> GoogleSignInException.Unknown(e.message)
        }
    }
}
