package mega.privacy.android.domain.usecase.transfers.overquota

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import javax.inject.Inject

/**
 * Monitor Storage Over Quota use case
 */
class MonitorStorageOverQuotaUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
) {

    /**
     * Invoke
     */
    operator fun invoke(): Flow<Boolean> {
        val storageOverQuotaFlow = transferRepository.monitorStorageOverQuota()
        val storageStateFlow = monitorStorageStateEventUseCase()

        return combine(
            storageOverQuotaFlow,
            storageStateFlow,
        ) { storageOverQuota, storageStateEvent ->
            storageOverQuota
                    || storageStateEvent.storageState == StorageState.Red
                    || storageStateEvent.storageState == StorageState.PayWall
        }
    }
}
