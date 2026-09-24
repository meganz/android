package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Rename Recovery Key File Use Case
 *
 */
class RenameRecoveryKeyFileUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(relativePath: String, newName: String) =
        accountRepository.renameRecoveryKeyFile(relativePath, newName)
}