package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.usecase.login.ClearRestoreCredentialUseCase
import javax.inject.Inject

/**
 * Clear restore credential logout task
 *
 * [mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase] also clears the credential,
 * but on a user logout it is only reached through the chat SDK logout callback. Clearing here as
 * well means a missed callback cannot leave a credential that signs the user back in after a
 * device restore.
 */
class ClearRestoreCredentialLogoutTask @Inject constructor(
    private val clearRestoreCredentialUseCase: ClearRestoreCredentialUseCase,
) : LogoutTask {

    override suspend fun onLogoutSuccess() {
        clearRestoreCredentialUseCase()
    }
}
