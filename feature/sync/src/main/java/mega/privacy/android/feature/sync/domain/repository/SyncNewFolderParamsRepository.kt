package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

internal interface SyncNewFolderParamsRepository {

    fun setSelectedMegaFolder(megaFolder: RemoteFolder)

    fun monitorSelectedMegaFolder(): Flow<RemoteFolder?>
}
