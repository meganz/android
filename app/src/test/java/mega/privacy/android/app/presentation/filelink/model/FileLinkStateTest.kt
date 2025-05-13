package mega.privacy.android.app.presentation.filelink.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.folderlink.model.LinkErrorState
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class FileLinkStateTest {
    private lateinit var underTest: FileLinkState

    @Test
    fun `test that when typed node has preview then icon resource is not set`() {
        val iconResource = 123
        val filePreviewPath = "data/cache/xyz.jpg"
        val fileName = "abc"
        val fileSize = 123L
        val fileNode = mock<TypedFileNode> {
            on { name }.thenReturn(fileName)
            on { size }.thenReturn(fileSize)
            on { previewPath }.thenReturn(filePreviewPath)
        }
        underTest = FileLinkState()
        val result = underTest.copyWithTypedNode(fileNode, iconResource)
        assertThat(result.title).isEqualTo(fileName)
        assertThat(result.sizeInBytes).isEqualTo(fileSize)
        assertThat(result.previewPath).isEqualTo(filePreviewPath)
        assertThat(result.iconResource).isNull()
    }

    @Test
    fun `test that when typed node does not have preview then icon resource is set`() {
        val iconResource = 123
        val filePreviewPath = null
        val fileNode = mock<TypedFileNode> {
            on { name }.thenReturn("abc")
            on { size }.thenReturn(123)
            on { previewPath }.thenReturn(filePreviewPath)
        }
        underTest = FileLinkState()
        val result = underTest.copyWithTypedNode(fileNode, iconResource)
        assertThat(result.previewPath).isNull()
        assertThat(result.iconResource).isEqualTo(iconResource)
    }

    @Test
    fun `test that showContentActions is true when nodes are fetched without errors`() {
        val folderLinkState = FileLinkState(
            jobInProgressState = null,
            errorState = LinkErrorState.NoError
        )
        assertThat(folderLinkState.showContentActions).isTrue()
    }


    @Test
    fun `test that isLoading is true when jobInProgressState is InitialLoading`() {
        val folderLinkState = FileLinkState(
            jobInProgressState = FileLinkJobInProgressState.InitialLoading,
        )
        assertThat(folderLinkState.isLoading).isTrue()
    }
}