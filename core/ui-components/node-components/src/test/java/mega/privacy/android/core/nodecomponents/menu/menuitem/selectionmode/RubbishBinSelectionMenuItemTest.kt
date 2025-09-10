package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RubbishBinSelectionMenuItemTest {

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
        val rubbishBinMenuItem = RubbishBinSelectionMenuItem(mock<TrashMenuAction>())

        val result = rubbishBinMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `test shouldDisplay returns true with multiple nodes when all conditions are met`() =
        runTest {
            val rubbishBinMenuItem = RubbishBinSelectionMenuItem(mock<TrashMenuAction>())

            val result = rubbishBinMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockFileNode, mockFolderNode),
                canBeMovedToTarget = true,
                noNodeInBackups = true,
                noNodeTakenDown = true
            )

            assertThat(result).isTrue()
        }

    @Test
    fun `test shouldDisplay returns false when node is in backups`() = runTest {
        val rubbishBinMenuItem = RubbishBinSelectionMenuItem(mock<TrashMenuAction>())

        val result = rubbishBinMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = false,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when cannot be moved to target`() = runTest {
        val rubbishBinMenuItem = RubbishBinSelectionMenuItem(mock<TrashMenuAction>())

        val result = rubbishBinMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = false,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when no access permission`() = runTest {
        val rubbishBinMenuItem = RubbishBinSelectionMenuItem(mock<TrashMenuAction>())

        val result = rubbishBinMenuItem.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when any node is incoming share`() = runTest {
        val rubbishBinMenuItem = RubbishBinSelectionMenuItem(mock<TrashMenuAction>())

        val result = rubbishBinMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockIncomingShareNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test shouldDisplay returns false when mixed incoming and non-incoming share nodes`() =
        runTest {
            val rubbishBinMenuItem = RubbishBinSelectionMenuItem(mock<TrashMenuAction>())

            val result = rubbishBinMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockFileNode, mockIncomingShareNode),
                canBeMovedToTarget = true,
                noNodeInBackups = true,
                noNodeTakenDown = true
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test shouldDisplay ignores noNodeTakenDown parameter`() = runTest {
        val rubbishBinMenuItem = RubbishBinSelectionMenuItem(mock<TrashMenuAction>())

        // Test that it ignores noNodeTakenDown parameter
        val result1 = rubbishBinMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true
        )

        val result2 = rubbishBinMenuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = false
        )

        assertThat(result1).isTrue()
        assertThat(result2).isTrue() // Should be the same regardless of noNodeTakenDown
    }
}
