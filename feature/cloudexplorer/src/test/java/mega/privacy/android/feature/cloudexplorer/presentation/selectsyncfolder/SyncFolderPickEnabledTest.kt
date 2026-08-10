package mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import org.junit.jupiter.api.Test

internal class SyncFolderPickEnabledTest {

    @Test
    fun `test that stop backup is always selectable even when select is not enabled`() {
        assertThat(
            syncFolderPickEnabled(
                explorerMode = ExplorerMode.SelectStopBackupDestination,
                isSelectEnabled = false,
            )
        ).isTrue()
    }

    @Test
    fun `test that a sync folder is selectable when select is enabled`() {
        assertThat(
            syncFolderPickEnabled(
                explorerMode = ExplorerMode.SelectSyncFolder,
                isSelectEnabled = true,
            )
        ).isTrue()
    }

    @Test
    fun `test that a sync folder is not selectable when select is disabled`() {
        assertThat(
            syncFolderPickEnabled(
                explorerMode = ExplorerMode.SelectSyncFolder,
                isSelectEnabled = false,
            )
        ).isFalse()
    }
}
