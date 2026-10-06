package mega.privacy.android.data.repository.security

import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.restorecredential.CreateRestoreCredentialDomException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.RestoreCredentialGateway
import mega.privacy.android.domain.entity.login.CreatedRestoreCredential
import mega.privacy.android.domain.exception.login.RestoreCredentialException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.security.RestoreCredentialRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation of [RestoreCredentialRepository]
 */
internal class RestoreCredentialRepositoryImpl @Inject constructor(
    private val restoreCredentialGateway: RestoreCredentialGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RestoreCredentialRepository {

    override suspend fun createRestoreCredential(requestJson: String): CreatedRestoreCredential =
        withContext(ioDispatcher) {
            mapRestoreCredentialErrors {
                try {
                    CreatedRestoreCredential(
                        responseJson = restoreCredentialGateway.createRestoreCredential(
                            requestJson = requestJson,
                            isCloudBackupEnabled = true,
                        ),
                        isBackedUpToCloud = true,
                    )
                } catch (e: E2eeUnavailableException) {
                    // Cloud backup needs Android Backup and a screen lock; without them the key
                    // can still be kept on this device.
                    Timber.w(e, "Cloud backup unavailable, storing restore credential locally")
                    CreatedRestoreCredential(
                        responseJson = restoreCredentialGateway.createRestoreCredential(
                            requestJson = requestJson,
                            isCloudBackupEnabled = false,
                        ),
                        isBackedUpToCloud = false,
                    )
                }
            }
        }

    override suspend fun getRestoreCredential(requestJson: String): String =
        withContext(ioDispatcher) {
            mapRestoreCredentialErrors {
                restoreCredentialGateway.getRestoreCredential(requestJson)
            }
        }

    override suspend fun clearRestoreCredential() =
        withContext(ioDispatcher) {
            mapRestoreCredentialErrors {
                restoreCredentialGateway.clearRestoreCredential()
            }
        }

    private inline fun <T> mapRestoreCredentialErrors(block: () -> T): T =
        try {
            block()
        } catch (e: NoCredentialException) {
            throw RestoreCredentialException.NoCredential(e)
        } catch (e: CreateRestoreCredentialDomException) {
            throw RestoreCredentialException.InvalidRequest(e)
        } catch (e: IllegalArgumentException) {
            throw RestoreCredentialException.InvalidRequest(e)
        } catch (e: CreateCredentialException) {
            throw RestoreCredentialException.Failed(e)
        } catch (e: GetCredentialException) {
            throw RestoreCredentialException.Failed(e)
        } catch (e: ClearCredentialException) {
            throw RestoreCredentialException.Failed(e)
        }
}
