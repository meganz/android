package mega.privacy.android.core.nodecomponents.menu.menuitem

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrashBottomSheetMenuItemTest {

    private val nodeHandlesToJsonMapper = mock<NodeHandlesToJsonMapper>()
    private val underTest = TrashBottomSheetMenuItem(
        menuAction = mock<TrashMenuAction>(),
        nodeHandlesToJsonMapper = nodeHandlesToJsonMapper
    )

    @Test
    fun `test that shouldDisplay returns true when node has OWNER permission and not in rubbish`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { isIncomingShare } doReturn false
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            }
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = AccessPermission.OWNER,
                isInBackups = false,
                node = node,
                isConnected = true
            )
            assertThat(result).isTrue()
        }

    @Test
    fun `test that shouldDisplay returns true when node has FULL permission and not in rubbish`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { isIncomingShare } doReturn false
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            }
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = AccessPermission.FULL,
                isInBackups = false,
                node = node,
                isConnected = true
            )
            assertThat(result).isTrue()
        }

    @Test
    fun `test that shouldDisplay returns false when node is in rubbish`() = runTest {
        val node = mock<TypedFileNode> {
            on { isIncomingShare } doReturn false
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = true,
            accessPermission = AccessPermission.OWNER,
            isInBackups = false,
            node = node,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is incoming share`() = runTest {
        val node = mock<TypedFolderNode> {
            on { isIncomingShare } doReturn true
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackups = false,
            node = node,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when access permission is READWRITE`() = runTest {
        val node = mock<TypedFileNode> {
            on { isIncomingShare } doReturn false
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.READWRITE,
            isInBackups = false,
            node = node,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is in backups`() = runTest {
        val node = mock<TypedFileNode> {
            on { isIncomingShare } doReturn false
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackups = true,
            node = node,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is S4 container`() = runTest {
        val folderNode = mock<TypedFolderNode> {
            on { isIncomingShare } doReturn false
            on { isTakenDown } doReturn false
            on { isS4Container } doReturn true
            on { isNodeKeyDecrypted } doReturn true
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackups = false,
            node = folderNode,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node key is not decrypted`() = runTest {
        val node = mock<TypedFileNode> {
            on { isIncomingShare } doReturn false
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn false
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackups = false,
            node = node,
            isConnected = true
        )
        assertThat(result).isFalse()
    }
}

