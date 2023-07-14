package mega.privacy.android.feature.sync.ui.mapper

import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.ui.model.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import javax.inject.Inject

internal class SyncUiItemMapper @Inject constructor() {

    operator fun invoke(folderPairs: List<FolderPair>): List<SyncUiItem> =
        folderPairs.map { invoke(it) }

    operator fun invoke(folderPair: FolderPair): SyncUiItem =
        SyncUiItem(
            folderPair.id,
            folderPair.pairName,
            SyncStatus.SYNCING, // the status conversion will be handled later
            folderPair.localFolderPath,
            folderPair.remoteFolder.name,
            "Two-way sync",
            false
        )
}