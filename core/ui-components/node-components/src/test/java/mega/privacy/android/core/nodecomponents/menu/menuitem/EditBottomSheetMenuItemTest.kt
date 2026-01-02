package mega.privacy.android.core.nodecomponents.menu.menuitem

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.EditMenuAction
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EditBottomSheetMenuItemTest {

    private val getFileTypeInfoUseCase = mock<GetFileTypeInfoUseCase>()
    private val underTest = EditBottomSheetMenuItem(
        menuAction = mock<EditMenuAction>(),
        getFileTypeInfoUseCase = getFileTypeInfoUseCase
    )

    @Test
    fun `test that shouldDisplay returns true when node is text file with valid permissions`() =
        runTest {
            val fileNode = mock<TypedFileNode> {
                on { fullSizePath } doReturn "/path/to/file.txt"
                on { isTakenDown } doReturn false
                on { isNodeKeyDecrypted } doReturn true
            }
            val file = File("/path/to/file.txt")
            whenever(getFileTypeInfoUseCase(file)).thenReturn(TextFileTypeInfo("text/plain", "txt"))
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = AccessPermission.OWNER,
                isInBackups = false,
                node = fileNode,
                isConnected = true
            )
            assertThat(result).isTrue()
        }

    @Test
    fun `test that shouldDisplay returns false when node is in rubbish`() = runTest {
        val fileNode = mock<TypedFileNode> {
            on { fullSizePath } doReturn "/path/to/file.txt"
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        val file = File("/path/to/file.txt")
        whenever(getFileTypeInfoUseCase(file)).thenReturn(TextFileTypeInfo("text/plain", "txt"))
        val result = underTest.shouldDisplay(
            isNodeInRubbish = true,
            accessPermission = AccessPermission.OWNER,
            isInBackups = false,
            node = fileNode,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is in backups`() = runTest {
        val fileNode = mock<TypedFileNode> {
            on { fullSizePath } doReturn "/path/to/file.txt"
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        val file = File("/path/to/file.txt")
        whenever(getFileTypeInfoUseCase(file)).thenReturn(TextFileTypeInfo("text/plain", "txt"))
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackups = true,
            node = fileNode,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is taken down`() = runTest {
        val fileNode = mock<TypedFileNode> {
            on { fullSizePath } doReturn "/path/to/file.txt"
            on { isTakenDown } doReturn true
            on { isNodeKeyDecrypted } doReturn true
        }
        val file = File("/path/to/file.txt")
        whenever(getFileTypeInfoUseCase(file)).thenReturn(TextFileTypeInfo("text/plain", "txt"))
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackups = false,
            node = fileNode,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when access permission is READ`() = runTest {
        val fileNode = mock<TypedFileNode> {
            on { fullSizePath } doReturn "/path/to/file.txt"
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn true
        }
        val file = File("/path/to/file.txt")
        whenever(getFileTypeInfoUseCase(file)).thenReturn(TextFileTypeInfo("text/plain", "txt"))
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.READ,
            isInBackups = false,
            node = fileNode,
            isConnected = true
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is not a FileNode`() = runTest {
        val folderNode = mock<TypedFolderNode> {
            on { isTakenDown } doReturn false
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
        val fileNode = mock<TypedFileNode> {
            on { fullSizePath } doReturn "/path/to/file.txt"
            on { isTakenDown } doReturn false
            on { isNodeKeyDecrypted } doReturn false
        }
        val file = File("/path/to/file.txt")
        whenever(getFileTypeInfoUseCase(file)).thenReturn(TextFileTypeInfo("text/plain", "txt"))
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackups = false,
            node = fileNode,
            isConnected = true
        )
        assertThat(result).isFalse()
    }
}

