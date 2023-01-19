package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.StorageState
import javax.inject.Inject

/**
 * Default is storage over quota
 *
 * @property monitorStorageStateEvent
 */
class DefaultIsStorageOverQuota @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent
) : IsStorageOverQuota {
    override suspend fun invoke() =
        monitorStorageStateEvent().value.storageState == StorageState.PayWall
}