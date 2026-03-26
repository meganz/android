package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.shares.IsOutShareUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareFolderSelectionMenuItemTest {

    private val isOutShareUseCase = mock<IsOutShareUseCase> {
        onBlocking { invoke(any()) } doReturn false
    }

    private val underTest = ShareFolderSelectionMenuItem(
        mock<ShareFolderMenuAction>(),
        isOutShareUseCase
    )

    private val mockFolderNode = mock<TypedFolderNode> {
        on { id } doReturn NodeId(123L)
        on { isTakenDown } doReturn false
        on { isS4Container } doReturn false
    }

    @Test
    fun `test that shouldDisplay returns true when all conditions are met`() = runTest {
        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mockFolderNode),
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `test that shouldDisplay returns false when node is S4 container`() = runTest {
        val s4ContainerNode = mock<TypedFolderNode> {
            on { id } doReturn NodeId(456L)
            on { isTakenDown } doReturn false
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
