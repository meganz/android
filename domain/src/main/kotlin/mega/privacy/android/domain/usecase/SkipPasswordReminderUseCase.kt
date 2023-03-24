package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject


/**
 * Use Case to notify the user has successfully skipped the password check
 */
class SkipPasswordReminderUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    /**
     * Invoke the Use case
     */
    suspend operator fun invoke() {
        repository.skipPasswordReminderDialog()
    }
}