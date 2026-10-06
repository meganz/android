package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.entity.login.CreatedRestoreCredential
import mega.privacy.android.domain.repository.security.RestoreCredentialRepository
import javax.inject.Inject

/**
 * Creates the device restore credential, replacing any existing one.
 */
class CreateRestoreCredentialUseCase @Inject constructor(
    private val restoreCredentialRepository: RestoreCredentialRepository,
) {

    /**
     * Invoke.
     *
     * @param requestJson Registration request from the server
     * @return the registration response and whether the credential is backed up to the cloud
     */
    suspend operator fun invoke(requestJson: String): CreatedRestoreCredential =
        restoreCredentialRepository.createRestoreCredential(requestJson)
}
