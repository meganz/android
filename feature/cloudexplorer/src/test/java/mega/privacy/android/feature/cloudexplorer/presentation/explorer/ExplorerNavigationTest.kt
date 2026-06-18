package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.destination.CopyNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExplorerNavigationTest {

    @Test
    fun `test that navigateToFolder navigates with a NodesExplorerNavKey carrying the tapped folder`() {
        val captured = mutableListOf<NavKey>()
        val navigate = navigateToFolder(
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            explorerMode = ExplorerMode.Copy,
            startNavKey = START_NAV_KEY,
            shareUris = null,
            disabledNodeIds = listOf(DISABLED_NODE_ID),
            protectedUserTap = { it() },
            onNavigate = { captured.add(it) },
        )

        navigate(TAPPED_FOLDER_ID)

        val key = captured.single() as NodesExplorerNavKey
        assertThat(key.nodeId).isEqualTo(TAPPED_FOLDER_ID)
        assertThat(key.nodeSourceType).isEqualTo(NodeSourceType.CLOUD_DRIVE)
        assertThat(key.explorerMode).isEqualTo(ExplorerMode.Copy)
        assertThat(key.startNavKey).isEqualTo(START_NAV_KEY)
        assertThat(key.disabledNodeIds).containsExactly(DISABLED_NODE_ID)
    }

    @Test
    fun `test that navigateToFolder does not navigate when protectedUserTap blocks the action`() {
        val captured = mutableListOf<NavKey>()
        val navigate = navigateToFolder(
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            explorerMode = ExplorerMode.Copy,
            startNavKey = START_NAV_KEY,
            shareUris = null,
            protectedUserTap = { /* blocked: never invokes the action */ },
            onNavigate = { captured.add(it) },
        )

        navigate(TAPPED_FOLDER_ID)

        assertThat(captured).isEmpty()
    }

    @Test
    fun `test that navigateToFolderPath navigates once per folder id in order`() {
        val captured = mutableListOf<NavKey>()
        val navigatePath = navigateToFolderPath(
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            explorerMode = ExplorerMode.Copy,
            startNavKey = START_NAV_KEY,
            shareUris = null,
            protectedUserTap = { it() },
            onNavigate = { captured.add(it) },
        )

        navigatePath(listOf(NodeId(1), NodeId(2)))

        assertThat(captured.map { (it as NodesExplorerNavKey).nodeId })
            .containsExactly(NodeId(1), NodeId(2))
            .inOrder()
    }

    private companion object {
        val START_NAV_KEY = CopyNavKey(emptyList())
        val TAPPED_FOLDER_ID = NodeId(42)
        val DISABLED_NODE_ID = NodeId(7)
    }
}
