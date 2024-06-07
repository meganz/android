package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastStorageOverQuotaUseCase
import javax.inject.Inject

/**
 * Check if the storage is over quota
 */
class IsStorageOverQuotaUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val broadcastStorageOverQuotaUseCase: BroadcastStorageOverQuotaUseCase,
) {
    /**
     * Calculate the percentage of storage used,
     * with 100 meaning the storage is full and the user has to upgrade to continue uploading files
     */
    suspend operator fun invoke(): Boolean {
        val storageUsedPercentage =
            (100 * accountRepository.getUsedStorage() / accountRepository.getMaxStorage()).toInt()
        return (storageUsedPercentage >= FULL_STORAGE_PERCENTAGE).also {
            broadcastStorageOverQuotaUseCase(it)
        }
    }

    private companion object {
        const val FULL_STORAGE_PERCENTAGE = 100
    }
}
