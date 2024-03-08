package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import javax.inject.Inject

/**
 * Check if the storage is over quota
 *
 * @property monitorStorageStateEventUseCase
 */
class IsStorageOverQuotaUseCase @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
) {
    /**
     * Invoke
     */
    operator fun invoke() =
        monitorStorageStateEventUseCase().value.storageState == StorageState.Red
}
