package mega.privacy.android.feature.cloudexplorer.presentation.components

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.nodes.components.previewdata.previewFileNodeUiItem
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SelectableNodeIdsTest {
    private val folder = previewFolderNodeUiItem(FOLDER_ID.longValue)
    private val file = previewFileNodeUiItem(FILE_ID.longValue)

    @Test
    fun `test that folders are excluded by default`() {
        val result = selectableNodeIds(
            items = listOf(folder, file),
            disabledNodeIds = emptySet(),
            videosOnly = false,
        )

        assertThat(result).containsExactly(FILE_ID)
    }

    @Test
    fun `test that folders are included when folder selection is allowed`() {
        val result = selectableNodeIds(
            items = listOf(folder, file),
            disabledNodeIds = emptySet(),
            videosOnly = false,
            allowsFolderSelection = true,
        )

        assertThat(result).containsExactly(FOLDER_ID, FILE_ID)
    }

    private companion object {
        val FOLDER_ID = NodeId(1L)
        val FILE_ID = NodeId(10L)
    }
}
