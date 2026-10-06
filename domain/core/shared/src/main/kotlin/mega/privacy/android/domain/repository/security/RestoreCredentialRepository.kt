package mega.privacy.android.domain.repository.security

import mega.privacy.android.domain.entity.login.CreatedRestoreCredential
import mega.privacy.android.domain.exception.login.RestoreCredentialException

/**
 * Repository for the device restore credential, the key that lets the app sign the user in
 * again without interaction after the app is restored onto a new device.
 *
 * Request and response JSON are opaque strings exchanged with the server and are never parsed
 * on the client.
 */
interface RestoreCredentialRepository {

    /**
     * Creates the restore credential, replacing any existing one. It is included in the cloud
     * backup when possible and kept on this device only otherwise.
     *
     * @param requestJson Registration request from the server
     * @return the registration response and whether the credential is backed up to the cloud
     * @throws RestoreCredentialException.InvalidRequest if the request is rejected
     * @throws RestoreCredentialException.Failed for any other failure
     */
    suspend fun createRestoreCredential(requestJson: String): CreatedRestoreCredential

    /**
     * Gets the restore credential without showing any UI.
     *
     * @param requestJson Authentication request from the server
     * @return the authentication response to send to the server
     * @throws RestoreCredentialException.NoCredential if the device holds no restore credential
     * @throws RestoreCredentialException.InvalidRequest if the request is rejected
     * @throws RestoreCredentialException.Failed for any other failure
     */
    suspend fun getRestoreCredential(requestJson: String): String

    /**
     * Deletes the restore credential. It does nothing if none exists.
     *
     * @throws RestoreCredentialException.Failed if the platform fails to clear it
     */
    suspend fun clearRestoreCredential()
}
