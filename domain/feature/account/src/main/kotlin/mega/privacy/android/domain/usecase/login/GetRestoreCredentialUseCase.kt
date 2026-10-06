package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.RestoreCredentialRepository
import javax.inject.Inject

/**
 * Gets the device restore credential without showing any UI.
 */
class GetRestoreCredentialUseCase @Inject constructor(
    private val restoreCredentialRepository: RestoreCredentialRepository,
) {

    /**
     * Invoke.
     *
     * @param requestJson Authentication request from the server
     * @return the authentication response to send to the server
     */
    suspend operator fun invoke(requestJson: String): String =
        restoreCredentialRepository.getRestoreCredential(requestJson)
}
