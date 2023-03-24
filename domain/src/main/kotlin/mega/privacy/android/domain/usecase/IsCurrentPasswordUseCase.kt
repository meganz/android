package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Check is password the current password
 */
class IsCurrentPasswordUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    /**
     * Invoke and returns true if password is the same as current password, else false
     */
    suspend operator fun invoke(password: String): Boolean = repository.isCurrentPassword(password)
}