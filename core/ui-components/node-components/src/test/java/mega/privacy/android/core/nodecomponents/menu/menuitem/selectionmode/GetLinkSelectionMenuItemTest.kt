package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.GetLinkMenuAction
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class GetLinkSelectionMenuItemTest {
    @Test
    fun `test shouldDisplay returns true when all conditions are met and nodeSourceType is not Links`() =
        runTest {
            val mockNode1 = mock<TypedFileNode> {
                on { exportedData }.thenReturn(mock())
            }
            val mockNode2 = mock<TypedFileNode> {
                on { exportedData }.thenReturn(mock())
            }
            val getLinkMenuItem = GetLinkSelectionMenuItem(mock<GetLinkMenuAction>())

            val result = getLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockNode1, mockNode2),
                canBeMovedToTarget = false,
                noNodeInBackups = true,
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
            val getLinkMenuItem = GetLinkSelectionMenuItem(mock<GetLinkMenuAction>())

            val result = getLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockNode),
                canBeMovedToTarget = false,
                noNodeInBackups = true,
                noNodeTakenDown = false
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test shouldDisplay returns false when selectedNode exportedData is not null and nodeSourceType is not Links`() =
        runTest {
            val mockNode = mock<TypedFileNode> {
                on { exportedData }.thenReturn(mock())
            }
            val getLinkMenuItem = GetLinkSelectionMenuItem(mock<GetLinkMenuAction>())

            val result = getLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockNode),
                canBeMovedToTarget = false,
                noNodeInBackups = true,
                noNodeTakenDown = true
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test shouldDisplay returns false when noNodeInBackups is false and nodeSourceType is not Links`() =
        runTest {
            val getLinkMenuItem = GetLinkSelectionMenuItem(mock<GetLinkMenuAction>())

            val result = getLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mock()),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test shouldDisplay returns false when hasNodeAccessPermission is false and nodeSourceType is not Links`() =
        runTest {
            val getLinkMenuItem = GetLinkSelectionMenuItem(mock<GetLinkMenuAction>())

            val result = getLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = false,
                selectedNodes = listOf(mock()),
                canBeMovedToTarget = false,
                noNodeInBackups = true,
                noNodeTakenDown = true
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test shouldDisplay returns false when nodeSourceType is Links`() =
        runTest {
            val getLinkMenuItem = GetLinkSelectionMenuItem(mock<GetLinkMenuAction>())

            val result = getLinkMenuItem.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mock()),
                canBeMovedToTarget = false,
                noNodeInBackups = true,
                noNodeTakenDown = true,
                nodeSourceType = NodeSourceType.LINKS
            )

            assertThat(result).isTrue()
        }

    @Test
    fun `test shouldDisplay returns true when selectedNodes size is 1 and nodeSourceType is Links`() =
        runTest {
            val getLinkMenuItem = GetLinkSelectionMenuItem(mock<GetLinkMenuAction>())

            val result = getLinkMenuItem.shouldDisplay(
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