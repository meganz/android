package mega.privacy.android.data.gateway

/**
 * Gateway to the platform restore credential, a single per-app key held by the credential
 * manager that survives a device restore.
 *
 * Failures are reported with the androidx.credentials exceptions.
 */
internal interface RestoreCredentialGateway {

    /**
     * Creates the restore credential, replacing any existing one.
     *
     * @param requestJson Registration request from the server
     * @param isCloudBackupEnabled Whether the credential is included in the cloud backup
     * @return the registration response JSON
     */
    suspend fun createRestoreCredential(requestJson: String, isCloudBackupEnabled: Boolean): String

    /**
     * Gets the restore credential silently.
     *
     * @param requestJson Authentication request from the server
     * @return the authentication response JSON
     */
    suspend fun getRestoreCredential(requestJson: String): String

    /**
     * Deletes the restore credential, if any.
     */
    suspend fun clearRestoreCredential()
}
