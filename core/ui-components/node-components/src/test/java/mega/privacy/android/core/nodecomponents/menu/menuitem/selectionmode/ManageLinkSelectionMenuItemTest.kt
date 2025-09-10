package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.ExportedData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManageLinkSelectionMenuItemTest {

    private val mockFileNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
        on { isTakenDown } doReturn false
        on { exportedData } doReturn null
    }

    private val mockFileNodeWithLink = mock<TypedFileNode> {
        on { id } doReturn NodeId(789L)
        on { isTakenDown } doReturn false
        on { exportedData } doReturn mock<ExportedData>()
    }

    private val mockFolderNode = mock<TypedFolderNode> {
        on { id } doReturn NodeId(456L)
        on { isTakenDown } doReturn false
        on { exportedData } doReturn null
    }

    private val mockTakenDownNodeWithLink = mock<TypedFileNode> {
        on { id } doReturn NodeId(999L)
        on { isTakenDown } doReturn true
        on { exportedData } doReturn mock<ExportedData>()
    }

    @Test
    fun `test shouldDisplay returns true when all conditions are met`() = runTest {
        val manageLinkMenuItem = ManageLinkSelectionMenuItem(mock<ManageLinkMenuAction>())

        val result = manageLinkMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNodeWithLink),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `test shouldDisplay returns false when no access permission`() = runTest {
        val manageLinkMenuItem = ManageLinkSelectionMenuItem(mock<ManageLinkMenuAction>())

        val result = manageLinkMenuItem.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = listOf(mockFileNodeWithLink),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when node is taken down`() = runTest {
        val manageLinkMenuItem = ManageLinkSelectionMenuItem(mock<ManageLinkMenuAction>())

        val result = manageLinkMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNodeWithLink),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = false
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when multiple nodes selected`() = runTest {
        val manageLinkMenuItem = ManageLinkSelectionMenuItem(mock<ManageLinkMenuAction>())

        val result = manageLinkMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNodeWithLink, mockFolderNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when node has no exported data`() = runTest {
        val manageLinkMenuItem = ManageLinkSelectionMenuItem(mock<ManageLinkMenuAction>())

        val result = manageLinkMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when folder node has no exported data`() = runTest {
        val manageLinkMenuItem = ManageLinkSelectionMenuItem(mock<ManageLinkMenuAction>())

        val result = manageLinkMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFolderNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay ignores canBeMovedToTarget and noNodeInBackups parameters`() = runTest {
        val manageLinkMenuItem = ManageLinkSelectionMenuItem(mock<ManageLinkMenuAction>())

        // Test that it ignores canBeMovedToTarget and noNodeInBackups parameters
        val result1 = manageLinkMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNodeWithLink),
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = true
        )

        val result2 = manageLinkMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNodeWithLink),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result1).isTrue()
        assertThat(result2).isTrue() // Should be the same regardless of canBeMovedToTarget and noNodeInBackups
    }

    @Test
    fun `test shouldDisplay requires exactly one node with exported data`() = runTest {
        val manageLinkMenuItem = ManageLinkSelectionMenuItem(mock<ManageLinkMenuAction>())

        // Test with empty list
        val result1 = manageLinkMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = emptyList(),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        // Test with single node without exported data
        val result2 = manageLinkMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        // Test with single node with exported data
        val result3 = manageLinkMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNodeWithLink),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result1).isFalse() // Empty list
        assertThat(result2).isFalse() // No exported data
        assertThat(result3).isTrue() // Has exported data
    }
}
