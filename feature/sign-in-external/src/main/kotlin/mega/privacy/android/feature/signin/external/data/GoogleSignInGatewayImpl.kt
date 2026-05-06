package mega.privacy.android.feature.signin.external.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import mega.privacy.android.domain.exception.login.GoogleSignInException
import mega.privacy.android.feature.signin.external.BuildConfig
import mega.privacy.android.feature.signin.external.data.gateway.GoogleSignInGateway
import javax.inject.Inject

/**
 * Implementation of [GoogleSignInGateway] using Credential Manager API.
 */
internal class GoogleSignInGatewayImpl @Inject constructor(
    private val credentialManager: CredentialManager,
) : GoogleSignInGateway {

    override suspend fun getGoogleIdToken(activityContext: Context): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return runCatching {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        }.getOrElse { e ->
            throw when (e) {
                is GetCredentialCancellationException -> GoogleSignInException.Cancelled
                is NoCredentialException -> GoogleSignInException.NoCredential
                else -> GoogleSignInException.Unknown(e.message)
            }
        }
    }
}
