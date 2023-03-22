package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import javax.inject.Inject

/**
 * Default not enough quota
 *
 * @property monitorStorageStateEventUseCase
 */
class DefaultIsNotEnoughQuota @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
) : IsNotEnoughQuota {
    override suspend fun invoke() =
        monitorStorageStateEventUseCase().value.storageState == StorageState.Red
}
