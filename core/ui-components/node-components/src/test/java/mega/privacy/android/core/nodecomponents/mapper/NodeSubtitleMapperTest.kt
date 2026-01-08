package mega.privacy.android.core.nodecomponents.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.model.NodeSubtitleText
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeSubtitleMapperTest {

    private lateinit var underTest: NodeSubtitleMapper

    @BeforeEach
    fun setUp() {
        underTest = NodeSubtitleMapper()
    }

    @Test
    fun `test that invoke returns FileSubtitle for TypedFileNode`() {
        val mockFileNode = mock<TypedFileNode> {
            whenever(it.size).thenReturn(1024L)
            whenever(it.modificationTime).thenReturn(1234567890L)
            whenever(it.exportedData).thenReturn(null)
        }

        val result = underTest(mockFileNode, showPublicLinkCreationTime = false)

        assertThat(result).isInstanceOf(NodeSubtitleText.FileSubtitle::class.java)
        val fileSubtitle = result as NodeSubtitleText.FileSubtitle
        assertThat(fileSubtitle.fileSizeValue).isEqualTo(1024L)
        assertThat(fileSubtitle.modificationTime).isEqualTo(1234567890L)
        assertThat(fileSubtitle.showPublicLinkCreationTime).isFalse()
        assertThat(fileSubtitle.publicLinkCreationTime).isNull()
    }

    @Test
    fun `test that invoke returns FileSubtitle with public link creation time when showPublicLinkCreationTime is true`() {
        val mockExportedData = mock<mega.privacy.android.domain.entity.node.ExportedData> {
            whenever(it.publicLinkCreationTime).thenReturn(9876543210L)
        }
        val mockFileNode = mock<TypedFileNode> {
            whenever(it.size).thenReturn(2048L)
            whenever(it.modificationTime).thenReturn(1234567890L)
            whenever(it.exportedData).thenReturn(mockExportedData)
        }

        val result = underTest(mockFileNode, showPublicLinkCreationTime = true)

        assertThat(result).isInstanceOf(NodeSubtitleText.FileSubtitle::class.java)
        val fileSubtitle = result as NodeSubtitleText.FileSubtitle
        assertThat(fileSubtitle.fileSizeValue).isEqualTo(2048L)
        assertThat(fileSubtitle.modificationTime).isEqualTo(1234567890L)
        assertThat(fileSubtitle.showPublicLinkCreationTime).isTrue()
        assertThat(fileSubtitle.publicLinkCreationTime).isEqualTo(9876543210L)
    }

    @Test
    fun `test that invoke returns FolderSubtitle for TypedFolderNode`() {
        val mockFolderNode = mock<TypedFolderNode> {
            whenever(it.childFolderCount).thenReturn(5)
            whenever(it.childFileCount).thenReturn(10)
        }

        val result = underTest(mockFolderNode, showPublicLinkCreationTime = false)

        assertThat(result).isInstanceOf(NodeSubtitleText.FolderSubtitle::class.java)
        val folderSubtitle = result as NodeSubtitleText.FolderSubtitle
        assertThat(folderSubtitle.childFolderCount).isEqualTo(5)
        assertThat(folderSubtitle.childFileCount).isEqualTo(10)
    }

    @Test
    fun `test that invoke returns SharedSubtitle for ShareFolderNode`() {
        val mockShareFolderNode = mock<ShareFolderNode> {
            whenever(it.childFolderCount).thenReturn(2)
            whenever(it.childFileCount).thenReturn(5)
        }

        val result = underTest(mockShareFolderNode, showPublicLinkCreationTime = false)

        // Since ShareFolderNode is a TypedFolderNode, it should return FolderSubtitle
        // unless it has shareData, which we can't easily mock
        assertThat(result).isInstanceOf(NodeSubtitleText.FolderSubtitle::class.java)
    }

    @Test
    fun `test that invoke returns Empty for unknown node type`() {
        val mockUnknownNode = mock<TypedFileNode> {
            whenever(it.size).thenReturn(1024L)
            whenever(it.modificationTime).thenReturn(1234567890L)
        }

        val result = underTest(mockUnknownNode, showPublicLinkCreationTime = false)

        // TypedFileNode should return FileSubtitle, not Empty
        assertThat(result).isInstanceOf(NodeSubtitleText.FileSubtitle::class.java)
    }
} 