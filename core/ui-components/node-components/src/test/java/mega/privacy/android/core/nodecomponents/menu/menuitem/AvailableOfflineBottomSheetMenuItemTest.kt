package mega.privacy.android.core.nodecomponents.menu.menuitem

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.foldernode.IsFolderEmptyUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AvailableOfflineBottomSheetMenuItemTest {

    private val isFolderEmptyUseCase = mock<IsFolderEmptyUseCase>()
    private val underTest = AvailableOfflineBottomSheetMenuItem(
        menuAction = mock<AvailableOfflineMenuAction>(),
        isFolderEmptyUseCase = isFolderEmptyUseCase,
    )

    @Test
    fun `test that shouldDisplay returns true when node is not available offline and folder is not empty`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { isAvailableOffline } doReturn false
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            }
            whenever(isFolderEmptyUseCase(node)).thenReturn(false)
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = null,
                isInBackups = false,
                node = node,
                isConnected = true
            )
            assertThat(result).isTrue()
        }

    @Test
    fun `test that shouldDisplay returns false when node is available offline`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn true
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is taken down`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn false
            on { isTakenDown } doReturn true
            on { isNodeKeyDecrypted } doReturn true
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is in rubbish`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn false
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = true,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when folder is empty`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn false
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(true)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is S4 container`() = runTest {
        val folderNode = mock<TypedFolderNode> {
            on { isAvailableOffline } doReturn false
            on { isTakenDown } doReturn false
            on { isS4Container } doReturn true
            on { isNodeKeyDecrypted } doReturn true
        }
        whenever(isFolderEmptyUseCase(folderNode)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = folderNode,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node key is not decrypted`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn false
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn false
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )
        assertThat(result).isFalse()
    }
}

