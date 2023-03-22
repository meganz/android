package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import javax.inject.Inject

/**
 * Default is storage over quota
 *
 * @property monitorStorageStateEventUseCase
 */
class DefaultIsStorageOverQuota @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase
) : IsStorageOverQuota {
    override suspend fun invoke() =
        monitorStorageStateEventUseCase().value.storageState == StorageState.PayWall
}