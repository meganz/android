package mega.privacy.android.feature.sync.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.FolderPairState
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Subscribes to status updates of a sync with the given syncId
 */
fun interface ObserveSyncState {

    operator fun invoke(): Flow<FolderPairState>
}
