package mega.privacy.android.app.providers.documentprovider

import android.provider.DocumentsContract.Document
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for [CloudDriveDocumentRowMapper].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudDriveDocumentRowMapperTest {

    private val nodeIdToDocumentIdMapper: NodeIdToDocumentIdMapper = mock()
    private lateinit var underTest: CloudDriveDocumentRowMapper

    private companion object {
        private const val DOCUMENT_ID_PREFIX = "mega_cloud_drive_root"
    }

    @BeforeEach
    fun setUp() {
        underTest = CloudDriveDocumentRowMapper(nodeIdToDocumentIdMapper)
    }

    @Test
    fun `test that invoke TypedFolderNode with prefix returns row with correct documentId and folder fields`() {
        val folderNode: TypedFolderNode = mock()
        whenever(folderNode.id).thenReturn(NodeId(100L))
        whenever(folderNode.name).thenReturn("My Folder")
        whenever(folderNode.creationTime).thenReturn(2000L)
        whenever(nodeIdToDocumentIdMapper.invoke(NodeId(100L), DOCUMENT_ID_PREFIX))
            .thenReturn("$DOCUMENT_ID_PREFIX:100")
        val typedFolder = DefaultTypedFolderNode(folderNode, FolderType.Default)

        val row = underTest(typedFolder, DOCUMENT_ID_PREFIX)

        assertThat(row.documentId).isEqualTo("$DOCUMENT_ID_PREFIX:100")
        assertThat(row.displayName).isEqualTo("My Folder")
        assertThat(row.mimeType).isEqualTo(Document.MIME_TYPE_DIR)
        assertThat(row.size).isEqualTo(0L)
        assertThat(row.lastModified).isEqualTo(2000L)
        assertThat(row.flags).isEqualTo(0)
    }

    @Test
    fun `test that invoke TypedFileNode with prefix returns row with correct documentId and file fields`() {
        val fileNode: TypedFileNode = mock()
        val fileTypeInfo: PdfFileTypeInfo = mock()
        whenever(fileNode.id).thenReturn(NodeId(200L))
        whenever(fileNode.name).thenReturn("report.pdf")
        whenever(fileNode.size).thenReturn(4096L)
        whenever(fileNode.modificationTime).thenReturn(3000L)
        whenever(fileNode.type).thenReturn(fileTypeInfo)
        whenever(fileTypeInfo.mimeType).thenReturn("application/pdf")
        whenever(nodeIdToDocumentIdMapper.invoke(NodeId(200L), DOCUMENT_ID_PREFIX))
            .thenReturn("$DOCUMENT_ID_PREFIX:200")

        val typedFile = DefaultTypedFileNode(fileNode)

        val row = underTest(typedFile, DOCUMENT_ID_PREFIX)

        assertThat(row.documentId).isEqualTo("$DOCUMENT_ID_PREFIX:200")
        assertThat(row.displayName).isEqualTo("report.pdf")
        assertThat(row.mimeType).isEqualTo("application/pdf")
        assertThat(row.size).isEqualTo(4096L)
        assertThat(row.lastModified).isEqualTo(3000L)
        assertThat(row.flags).isEqualTo(0)
    }
}
