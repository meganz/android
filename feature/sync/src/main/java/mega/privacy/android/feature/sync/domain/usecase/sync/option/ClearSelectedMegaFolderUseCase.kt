package mega.privacy.android.feature.sync.domain.usecase.sync.option

import mega.privacy.android.feature.sync.domain.repository.SyncNewFolderParamsRepository
import javax.inject.Inject

internal class ClearSelectedMegaFolderUseCase @Inject constructor(
    private val repository: SyncNewFolderParamsRepository,
) {

    operator fun invoke() {
        repository.clearSelectedMegaFolder()
    }
}