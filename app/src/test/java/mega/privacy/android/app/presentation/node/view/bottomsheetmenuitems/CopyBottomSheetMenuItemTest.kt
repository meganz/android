package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.CopyMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CopyBottomSheetMenuItemTest {

    private val underTest = CopyBottomSheetMenuItem(menuAction = CopyMenuAction())

    private val node = mock<TypedFileNode> {
        on { isTakenDown } doReturn false
        on { isNodeKeyDecrypted } doReturn true
    }

    @Test
    fun `test that shouldDisplay returns true when node is not taken down and not in rubbish`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            }
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = null,
                isInBackups = false,
                node = node,
                isConnected = true
            )

            Truth.assertThat(result).isTrue()
        }

    @Test
    fun `test that shouldDisplay returns false when node is taken down`() = runTest {
        val node = mock<TypedFileNode> {
            on { isTakenDown } doReturn true
            on { isNodeKeyDecrypted } doReturn true
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )

        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is in rubbish`() = runTest {
        val result = underTest.shouldDisplay(
            isNodeInRubbish = true,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )

        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns true when node is in backups`() = runTest {
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = true,
            node = node,
            isConnected = true
        )

        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `test that shouldDisplay returns false when node is S4 container`() = runTest {
        val folderNode = mock<TypedFolderNode> {
            on { isTakenDown } doReturn false
            on { isS4Container } doReturn true
            on { isNodeKeyDecrypted } doReturn true
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = folderNode,
            isConnected = true
        )

        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node key is not decrypted`() = runTest {
        val node = mock<TypedFileNode> {
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn false
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )

        Truth.assertThat(result).isFalse()
    }

}