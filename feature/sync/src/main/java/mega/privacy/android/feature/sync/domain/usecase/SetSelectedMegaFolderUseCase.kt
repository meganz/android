package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.repository.SyncNewFolderParamsRepository
import javax.inject.Inject

internal class SetSelectedMegaFolderUseCase @Inject constructor(
    private val repository: SyncNewFolderParamsRepository,
) {

    operator fun invoke(megaFolder: RemoteFolder) {
        repository.setSelectedMegaFolder(megaFolder)
    }
}
