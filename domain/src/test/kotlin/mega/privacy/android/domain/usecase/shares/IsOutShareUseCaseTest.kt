package mega.privacy.android.domain.usecase.shares

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsOutShareUseCaseTest {
    private lateinit var underTest: IsOutShareUseCase

    @BeforeAll
    fun init() {
        underTest = IsOutShareUseCase()
    }

    @Test
    fun `test that returns true for folder node with pending share`() {
        val mockFolderNode = mock<FolderNode> {
            on { isPendingShare } doReturn true
            on { isShared } doReturn false
        }

        val result = underTest(mockFolderNode)

        assertThat(result).isTrue()
    }

    @Test
    fun `test that returns true for folder node with shared status`() {
        val mockFolderNode = mock<FolderNode> {
            on { isPendingShare } doReturn false
            on { isShared } doReturn true
        }

        val result = underTest(mockFolderNode)

        assertThat(result).isTrue()
    }

    @Test
    fun `test that returns true for folder node with both pending share and shared status`() {
        val mockFolderNode = mock<FolderNode> {
            on { isPendingShare } doReturn true
            on { isShared } doReturn true
        }

        val result = underTest(mockFolderNode)

        assertThat(result).isTrue()
    }

    @Test
    fun `test that returns false for folder node with no share status`() {
        val mockFolderNode = mock<FolderNode> {
            on { isPendingShare } doReturn false
            on { isShared } doReturn false
        }

        val result = underTest(mockFolderNode)

        assertThat(result).isFalse()
    }

    @Test
    fun `test that returns false for non-folder node even with share status`() {
        val mockFileNode = mock<TypedFileNode> {
            on { id } doReturn NodeId(123L)
        }

        val result = underTest(mockFileNode)

        assertThat(result).isFalse()
    }

    @Test
    fun `test that returns false for typed folder node with no share status`() {
        val mockTypedFolderNode = mock<TypedFolderNode> {
            on { id } doReturn NodeId(123L)
            on { isPendingShare } doReturn false
            on { isShared } doReturn false
        }

        val result = underTest(mockTypedFolderNode)

        assertThat(result).isFalse()
    }
}
