package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to Notify the user has successfully checked his password
 */
class NotifyPasswordCheckedUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    /**
     * Invoke the Use Case
     */
    suspend operator fun invoke() {
        repository.notifyPasswordChecked()
    }
}