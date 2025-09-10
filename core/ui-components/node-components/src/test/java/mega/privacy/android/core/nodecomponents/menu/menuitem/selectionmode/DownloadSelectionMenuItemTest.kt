package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadSelectionMenuItemTest {

    private val mockFileNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
        on { isTakenDown } doReturn false
    }

    private val mockTakenDownNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(999L)
        on { isTakenDown } doReturn true
    }

    @Test
    fun `test shouldDisplay returns true when noNodeTakenDown is true`() = runTest {
        val downloadMenuItem = DownloadSelectionMenuItem(mock<DownloadMenuAction>())

        val result = downloadMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `test shouldDisplay returns false when noNodeTakenDown is false`() = runTest {
        val downloadMenuItem = DownloadSelectionMenuItem(mock<DownloadMenuAction>())

        val result = downloadMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockTakenDownNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = false
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay ignores other parameters and only checks noNodeTakenDown`() = runTest {
        val downloadMenuItem = DownloadSelectionMenuItem(mock<DownloadMenuAction>())

        // Test with various combinations of other parameters - should only depend on noNodeTakenDown
        val result1 = downloadMenuItem.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = true
        )

        val result2 = downloadMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = false
        )

        assertThat(result1).isTrue() // Should be true because noNodeTakenDown = true
        assertThat(result2).isFalse() // Should be false because noNodeTakenDown = false
    }
}
