package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.repository.SyncNewFolderParamsRepository
import javax.inject.Inject

internal class SyncNewFolderParamsRepositoryImpl @Inject constructor() :
    SyncNewFolderParamsRepository {

    private var selectedMegaFolder = MutableStateFlow<RemoteFolder?>(null)

    override fun setSelectedMegaFolder(megaFolder: RemoteFolder) {
        selectedMegaFolder.value = megaFolder
    }

    override fun monitorSelectedMegaFolder(): Flow<RemoteFolder?> =
        selectedMegaFolder
}
