package mega.privacy.android.data.facade

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnknownException
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.RestoreCredentialGateway
import javax.inject.Inject

/**
 * [RestoreCredentialGateway] implementation backed by [CredentialManager].
 *
 * No UI is shown for restore credentials, so the application context is enough.
 */
internal class RestoreCredentialFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialManager: CredentialManager,
) : RestoreCredentialGateway {

    override suspend fun createRestoreCredential(
        requestJson: String,
        isCloudBackupEnabled: Boolean,
    ): String {
        val response = credentialManager.createCredential(
            context = context,
            request = CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled),
        )
        return (response as? CreateRestoreCredentialResponse)?.responseJson
            ?: throw CreateCredentialUnknownException("Unexpected response type: ${response.type}")
    }

    // The restore option must be the only option in the request, the platform rejects mixing it
    // with other credential types.
    override suspend fun getRestoreCredential(requestJson: String): String {
        val response = credentialManager.getCredential(
            context = context,
            request = GetCredentialRequest(listOf(GetRestoreCredentialOption(requestJson))),
        )
        return (response.credential as? RestoreCredential)?.authenticationResponseJson
            ?: throw GetCredentialUnknownException(
                "Unexpected credential type: ${response.credential.type}"
            )
    }

    override suspend fun clearRestoreCredential() {
        credentialManager.clearCredentialState(
            ClearCredentialStateRequest(ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL)
        )
    }
}
