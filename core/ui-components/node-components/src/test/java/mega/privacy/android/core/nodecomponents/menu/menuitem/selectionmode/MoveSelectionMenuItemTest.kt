package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MoveSelectionMenuItemTest {

    private val mockFileNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
        on { isTakenDown } doReturn false
        on { isIncomingShare } doReturn false
    }

    private val mockFolderNode = mock<TypedFolderNode> {
        on { id } doReturn NodeId(456L)
        on { isTakenDown } doReturn false
        on { isIncomingShare } doReturn false
    }

    private val mockIncomingShareNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(111L)
        on { isTakenDown } doReturn false
        on { isIncomingShare } doReturn true
    }

    @Test
    fun `test shouldDisplay returns true when all conditions are met`() = runTest {
        val moveMenuItem = MoveSelectionMenuItem(mock<MoveMenuAction>())

        val result = moveMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `test shouldDisplay returns true with multiple non-incoming share nodes`() = runTest {
        val moveMenuItem = MoveSelectionMenuItem(mock<MoveMenuAction>())

        val result = moveMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode, mockFolderNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `test shouldDisplay returns false when any node is incoming share`() = runTest {
        val moveMenuItem = MoveSelectionMenuItem(mock<MoveMenuAction>())

        val result = moveMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockIncomingShareNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when mixed incoming and non-incoming share nodes`() = runTest {
        val moveMenuItem = MoveSelectionMenuItem(mock<MoveMenuAction>())

        val result = moveMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode, mockIncomingShareNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when node is in backups`() = runTest {
        val moveMenuItem = MoveSelectionMenuItem(mock<MoveMenuAction>())

        val result = moveMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = false,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay ignores other parameters like access permission and canBeMovedToTarget`() = runTest {
        val moveMenuItem = MoveSelectionMenuItem(mock<MoveMenuAction>())

        // Test that it ignores hasNodeAccessPermission and canBeMovedToTarget
        val result1 = moveMenuItem.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = false,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        val result2 = moveMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = false,
            noNodeTakenDown = true
        )

        assertThat(result1).isTrue() // Should be true because no incoming shares and not in backups
        assertThat(result2).isFalse() // Should be false because in backups
    }
}
