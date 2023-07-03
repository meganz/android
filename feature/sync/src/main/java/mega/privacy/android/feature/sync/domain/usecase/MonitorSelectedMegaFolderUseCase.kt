package mega.privacy.android.feature.sync.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.repository.SyncNewFolderParamsRepository
import javax.inject.Inject

internal class MonitorSelectedMegaFolderUseCase @Inject constructor(
    private val repository: SyncNewFolderParamsRepository,
) {

    operator fun invoke(): Flow<RemoteFolder?> = repository.monitorSelectedMegaFolder()
}
