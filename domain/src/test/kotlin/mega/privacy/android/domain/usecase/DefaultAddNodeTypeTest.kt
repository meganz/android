package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAddNodeTypeTest {
    private lateinit var underTest: AddNodeType

    private val getFolderType =
        mock<GetFolderType> { onBlocking { invoke(any()) }.thenReturn(FolderType.Default) }

    @Before
    fun setUp() {
        underTest = DefaultAddNodeType(
            getFolderType = getFolderType
        )
    }

    @Test
    fun `test that typed file is returned if node file is passed in`() = runTest {
        val actual = underTest(mock<FileNode>())
        assertThat(actual).isInstanceOf(TypedFileNode::class.java)
    }

    @Test
    fun `test that typed folder is returned if node folder is passed`() = runTest {
        val actual = underTest(mock<FolderNode>())
        assertThat(actual).isInstanceOf(TypedFolderNode::class.java)
    }

    @Test
    fun `test that folder type is the type returned from get folder type use case`() = runTest {
        val expected = FolderType.ChatFilesFolder
        getFolderType.stub {
            onBlocking { invoke(any()) }.thenReturn(expected)
        }

        val actual = underTest(mock<FolderNode>()) as TypedFolderNode

        assertThat(actual.type).isEqualTo(expected)
    }
}
