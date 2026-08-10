package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareSelectionMenuItemTest {

    private val underTest = ShareSelectionMenuItem(mock<ShareMenuAction>())

    private val mockFileNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
    }

    @Test
    fun `test that shouldDisplay returns true when all conditions are met`() = runTest {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `test that shouldDisplay returns false when noNodeTakenDown is false`() = runTest {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFileNode),
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = false,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when selectedNodes is empty`() = runTest {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = emptyList(),
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when any node is S4 container`() = runTest {
        val s4ContainerNode = mock<TypedFolderNode> {
            on { id } doReturn NodeId(456L)
            on { isS4Container } doReturn true
        }

        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(s4ContainerNode),
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isFalse()
    }
}
