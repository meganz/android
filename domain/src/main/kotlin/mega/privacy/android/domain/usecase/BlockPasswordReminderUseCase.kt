package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to notify the user wants to totally disable the password check
 */
class BlockPasswordReminderUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    /**
     * Invoke the Use Case
     */
    suspend operator fun invoke() {
        repository.blockPasswordReminderDialog()
    }
}