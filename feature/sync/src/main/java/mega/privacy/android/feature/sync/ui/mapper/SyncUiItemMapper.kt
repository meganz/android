package mega.privacy.android.feature.sync.ui.mapper

import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import javax.inject.Inject

internal class SyncUiItemMapper @Inject constructor() {

    operator fun invoke(folderPairs: List<FolderPair>): List<SyncUiItem> =
        folderPairs.map { invoke(it) }

    operator fun invoke(folderPair: FolderPair): SyncUiItem =
        SyncUiItem(
            id = folderPair.id,
            folderPairName = folderPair.pairName,
            status = folderPair.syncStatus,
            hasStalledIssues = false,
            deviceStoragePath = folderPair.localFolderPath,
            megaStoragePath = folderPair.remoteFolder.name,
            method = R.string.sync_two_way,
            expanded = false
        )
}