package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.StorageState
import javax.inject.Inject

/**
 * Default not enough quota
 *
 * @property monitorStorageStateEvent
 */
class DefaultIsNotEnoughQuota @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
) : IsNotEnoughQuota {
    override suspend fun invoke() =
        monitorStorageStateEvent().value.storageState == StorageState.Red
}
