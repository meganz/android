package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Change user's password
 */
class ChangePasswordUseCase @Inject constructor(
    private val repository: AccountRepository
) {

    /**
     * Invoke
     * @param newPassword as user's new password
     * @return true if change password is successful, else false
     */
    suspend operator fun invoke(newPassword: String): Boolean =
        repository.changePassword(newPassword)
}