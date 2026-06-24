package mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerRestrictedNode
import org.junit.jupiter.api.Test

internal class SyncFolderPickEnabledTest {

    private fun restrictedNode(usedBySyncOrBackup: Boolean) = SyncFolderPickerRestrictedNode(
        nodeId = NodeId(1L),
        name = "folder",
        isUsedBySyncOrBackup = usedBySyncOrBackup,
    )

    @Test
    fun `test that stop backup is always selectable even at root with a sync used child`() {
        assertThat(
            syncFolderPickEnabled(
                explorerMode = ExplorerMode.SelectStopBackupDestination,
                isAtRoot = true,
                restrictedNodes = listOf(restrictedNode(usedBySyncOrBackup = true)),
            )
        ).isTrue()
    }

    @Test
    fun `test that the cloud drive root cannot be selected as a sync folder`() {
        assertThat(
            syncFolderPickEnabled(
                explorerMode = ExplorerMode.SelectSyncFolder,
                isAtRoot = true,
                restrictedNodes = emptyList(),
            )
        ).isFalse()
    }

    @Test
    fun `test that a sync folder with no restricted children is selectable`() {
        assertThat(
            syncFolderPickEnabled(
                explorerMode = ExplorerMode.SelectSyncFolder,
                isAtRoot = false,
                restrictedNodes = emptyList(),
            )
        ).isTrue()
    }

    @Test
    fun `test that a sync folder with a child used by sync or backup is not selectable`() {
        assertThat(
            syncFolderPickEnabled(
                explorerMode = ExplorerMode.SelectSyncFolder,
                isAtRoot = false,
                restrictedNodes = listOf(restrictedNode(usedBySyncOrBackup = true)),
            )
        ).isFalse()
    }

    @Test
    fun `test that a sync folder whose restricted children are only reserved folders is selectable`() {
        assertThat(
            syncFolderPickEnabled(
                explorerMode = ExplorerMode.SelectSyncFolder,
                isAtRoot = false,
                restrictedNodes = listOf(restrictedNode(usedBySyncOrBackup = false)),
            )
        ).isTrue()
    }
}
