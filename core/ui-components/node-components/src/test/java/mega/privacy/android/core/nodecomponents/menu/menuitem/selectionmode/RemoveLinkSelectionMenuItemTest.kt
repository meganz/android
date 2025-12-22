package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveLinkMenuAction
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveLinkSelectionMenuItemTest {
    @Test
    fun `test shouldDisplay returns true when all conditions are met and nodeSourceType is not Links`() =
        runTest {
            val mockNode = mock<TypedFileNode> {
                on { exportedData }.thenReturn(mock())
            }
            val removeLinkMenuItem = RemoveLinkSelectionMenuItem(mock<RemoveLinkMenuAction>())

            val result = removeLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockNode),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true
            )

            assertThat(result).isTrue()
        }

    @Test
    fun `test shouldDisplay returns false when noNodeTakenDown is false and nodeSourceType is not Links`() =
        runTest {
            val mockNode = mock<TypedFileNode> {
                on { exportedData }.thenReturn(mock())
            }
            val removeLinkMenuItem = RemoveLinkSelectionMenuItem(mock<RemoveLinkMenuAction>())

            val result = removeLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockNode),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = false
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test shouldDisplay returns false when selectedNodes size is more than 1 and nodeSourceType is not Links`() =
        runTest {
            val mockNode1 = mock<TypedFileNode> {
                on { exportedData }.thenReturn(mock())
            }
            val mockNode2 = mock<TypedFileNode> {
                on { exportedData }.thenReturn(mock())
            }
            val removeLinkMenuItem = RemoveLinkSelectionMenuItem(mock<RemoveLinkMenuAction>())

            val result = removeLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockNode1, mockNode2),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test shouldDisplay returns false when exportedData is null and nodeSourceType is not Links`() =
        runTest {
            val removeLinkMenuItem = RemoveLinkSelectionMenuItem(mock<RemoveLinkMenuAction>())

            val result = removeLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mock()),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test shouldDisplay returns true when all conditions are met and nodeSourceType is Links`() =
        runTest {
            val removeLinkMenuItem = RemoveLinkSelectionMenuItem(mock<RemoveLinkMenuAction>())

            val result = removeLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mock(), mock()),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true,
                nodeSourceType = NodeSourceType.LINKS
            )

            assertThat(result).isTrue()
        }

    @Test
    fun `test shouldDisplay returns false when selectedNodes size is 1 and nodeSourceType is Links`() =
        runTest {
            val removeLinkMenuItem = RemoveLinkSelectionMenuItem(mock<RemoveLinkMenuAction>())

            val result = removeLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mock()),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true,
                nodeSourceType = NodeSourceType.LINKS
            )

            assertThat(result).isFalse()
        }
}