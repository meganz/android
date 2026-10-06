package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.RestoreCredentialRepository
import javax.inject.Inject

/**
 * Deletes the device restore credential, so the app can no longer sign in with it.
 */
class ClearRestoreCredentialUseCase @Inject constructor(
    private val restoreCredentialRepository: RestoreCredentialRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        restoreCredentialRepository.clearRestoreCredential()
    }
}
