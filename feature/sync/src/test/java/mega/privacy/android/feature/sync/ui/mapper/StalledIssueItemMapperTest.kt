package mega.privacy.android.feature.sync.ui.mapper

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.StalledIssueDetailInfoMapper
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.StalledIssueItemMapper
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.StalledIssueResolutionActionMapper
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StalledIssueItemMapperTest {

    private val stalledIssueResolutionActionMapper: StalledIssueResolutionActionMapper = mock()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()
    private val stalledIssueDetailInfoMapper: StalledIssueDetailInfoMapper = mock()

    private lateinit var underTest: StalledIssueItemMapper

    @BeforeEach
    fun setUp() {
        reset(
            stalledIssueResolutionActionMapper,
            fileTypeIconMapper,
            stalledIssueDetailInfoMapper
        )
        underTest = StalledIssueItemMapper(
            stalledIssueResolutionActionMapper = stalledIssueResolutionActionMapper,
            fileTypeIconMapper = fileTypeIconMapper,
            stalledIssueDetailInfoMapper = stalledIssueDetailInfoMapper,
        )
    }

    @Test
    fun `test that StalledIssue is mapped to StalledIssueUiItem with folder node`() = runTest {
        val stalledIssue = StalledIssue(
            syncId = 1L,
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera/subfolder"),
            id = "1_3_0"
        )
        val folderNode: FolderNode = mock {
            on { name } doReturn "Camera"
        }
        val detailedInfo = StalledIssueDetailedInfo("Title", "Description")
        val resolutionActions = listOf<StalledIssueResolutionAction>()

        whenever(stalledIssueDetailInfoMapper(stalledIssue)).thenReturn(detailedInfo)
        whenever(stalledIssueResolutionActionMapper(StallIssueType.DownloadIssue, true))
            .thenReturn(resolutionActions)

        val result = underTest(stalledIssue, listOf(folderNode))

        assertThat(result.syncId).isEqualTo(1L)
        assertThat(result.nodeIds).isEqualTo(listOf(NodeId(3L)))
        assertThat(result.localPaths).isEqualTo(listOf("/storage/emulated/0/DCIM"))
        assertThat(result.issueType).isEqualTo(StallIssueType.DownloadIssue)
        assertThat(result.conflictName).isEqualTo("Title")
        assertThat(result.nodeNames).isEqualTo(listOf("Camera/subfolder"))
        assertThat(result.detailedInfo).isEqualTo(detailedInfo)
        assertThat(result.actions).isEqualTo(resolutionActions)
        assertThat(result.displayedName).isEqualTo("subfolder")
        assertThat(result.displayedPath).isEqualTo("Camera")
    }

    @Test
    fun `test that StalledIssue is mapped to StalledIssueUiItem with file node`() = runTest {
        val stalledIssue = StalledIssue(
            syncId = 2L,
            nodeIds = listOf(NodeId(4L)),
            localPaths = listOf("/storage/emulated/0/Documents"),
            issueType = StallIssueType.UploadIssue,
            conflictName = "conflicting file",
            nodeNames = listOf("document.pdf"),
            id = "1_3_0"
        )
        val fileTypeInfo = PdfFileTypeInfo
        val fileNode: FileNode = mock {
            on { name } doReturn "document.pdf"
            on { type } doReturn fileTypeInfo
        }
        val detailedInfo = StalledIssueDetailedInfo("File Title", "File Description")
        val resolutionActions = listOf<StalledIssueResolutionAction>()
        val expectedIcon = 12345

        whenever(stalledIssueDetailInfoMapper(stalledIssue)).thenReturn(detailedInfo)
        whenever(stalledIssueResolutionActionMapper(StallIssueType.UploadIssue, false))
            .thenReturn(resolutionActions)
        whenever(fileTypeIconMapper("pdf")).thenReturn(expectedIcon)

        val result = underTest(stalledIssue, listOf(fileNode))

        assertThat(result.icon).isEqualTo(expectedIcon)
        assertThat(result.displayedName).isEqualTo("document.pdf")
        assertThat(result.displayedPath).isEqualTo("")
        verify(fileTypeIconMapper)("pdf")
    }

    @Test
    fun `test that StalledIssue is mapped with no nodes and uses local path`() = runTest {
        mockStatic(Uri::class.java).use { uriMock ->
            val uriString = "content://documents/path/file.txt"
            val contentUriMock: Uri = mock {
                on { scheme } doReturn "content"
                on { pathSegments } doReturn listOf(
                    "tree",
                    "primary:path",
                    "document",
                    "primary:path",
                    "file.txt"
                )
            }

            whenever(contentUriMock.toString()).thenReturn(uriString)
            whenever(Uri.parse(uriString)).thenReturn(contentUriMock)
            val stalledIssue = StalledIssue(
                syncId = 3L,
                nodeIds = listOf(NodeId(5L)),
                localPaths = listOf(uriString),
                issueType = StallIssueType.DownloadIssue,
                conflictName = "conflicting item",
                nodeNames = emptyList(),
                id = "1_3_0"
            )
            val detailedInfo = StalledIssueDetailedInfo("No Node Title", "No Node Description")
            val resolutionActions = listOf<StalledIssueResolutionAction>()

            whenever(stalledIssueDetailInfoMapper(stalledIssue)).thenReturn(detailedInfo)
            whenever(stalledIssueResolutionActionMapper(StallIssueType.DownloadIssue, true))
                .thenReturn(resolutionActions)

            val result = underTest(stalledIssue, emptyList())

            assertThat(result.icon).isEqualTo(iconPackR.drawable.ic_generic_medium_solid)
            assertThat(result.displayedName).isEqualTo("file.txt")
            assertThat(result.displayedPath).isEqualTo("primary:path")
        }
    }

    @Test
    fun `test that StalledIssue is mapped with no nodes and file extension`() = runTest {
        val stalledIssue = StalledIssue(
            syncId = 4L,
            nodeIds = listOf(NodeId(6L)),
            localPaths = listOf("/storage/emulated/0/image.jpg"),
            issueType = StallIssueType.UploadIssue,
            conflictName = "conflicting image",
            nodeNames = listOf("image.jpg"),
            id = "1_3_0"
        )
        val detailedInfo = StalledIssueDetailedInfo("Image Title", "Image Description")
        val resolutionActions = listOf<StalledIssueResolutionAction>()
        val expectedIcon = 54321

        whenever(stalledIssueDetailInfoMapper(stalledIssue)).thenReturn(detailedInfo)
        whenever(stalledIssueResolutionActionMapper(StallIssueType.UploadIssue, true))
            .thenReturn(resolutionActions)
        whenever(fileTypeIconMapper("jpg")).thenReturn(expectedIcon)

        val result = underTest(stalledIssue, emptyList())

        assertThat(result.icon).isEqualTo(expectedIcon)
        verify(fileTypeIconMapper)("jpg")
    }

    @Test
    fun `test that StalledIssueUiItem is mapped back to StalledIssue`() {
        val stalledIssueUiItem = StalledIssueUiItem(
            syncId = 1L,
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
            icon = 0,
            detailedInfo = StalledIssueDetailedInfo("", ""),
            actions = emptyList(),
            displayedName = "Camera",
            displayedPath = "/storage/emulated/0/DCIM",
            id = "1_3_0"
        )

        val result = underTest(stalledIssueUiItem)

        assertThat(result.syncId).isEqualTo(1L)
        assertThat(result.nodeIds).isEqualTo(listOf(NodeId(3L)))
        assertThat(result.localPaths).isEqualTo(listOf("/storage/emulated/0/DCIM"))
        assertThat(result.issueType).isEqualTo(StallIssueType.DownloadIssue)
        assertThat(result.conflictName).isEqualTo("conflicting folder")
        assertThat(result.nodeNames).isEqualTo(listOf("Camera"))
    }

    @Test
    fun `test that mapping handles complex nested path correctly`() = runTest {
        val stalledIssue = StalledIssue(
            syncId = 5L,
            nodeIds = listOf(NodeId(7L)),
            localPaths = listOf("/storage/emulated/0/Documents"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "nested conflict",
            nodeNames = listOf("Documents/Projects/Android/file.kt"),
            id = "1_3_0"
        )
        val detailedInfo = StalledIssueDetailedInfo("Nested Title", "Nested Description")
        val resolutionActions = listOf<StalledIssueResolutionAction>()

        whenever(stalledIssueDetailInfoMapper(stalledIssue)).thenReturn(detailedInfo)
        whenever(stalledIssueResolutionActionMapper(StallIssueType.DownloadIssue, true))
            .thenReturn(resolutionActions)

        val result = underTest(stalledIssue, emptyList())

        assertThat(result.displayedName).isEqualTo("file.kt")
        assertThat(result.displayedPath).isEqualTo("Documents/Projects/Android")
    }

    @Test
    fun `test that Uri pathSegments drop logic works correctly with different segment counts`() =
        runTest {
            mockStatic(Uri::class.java).use { uriMock ->
                val uriString =
                    "content://documents/tree/primary:Downloads/document/primary:Downloads/folder1/folder2/file.txt"
                val contentUriMock: Uri = mock {
                    on { scheme } doReturn "content"
                    on { pathSegments } doReturn listOf(
                        "tree",
                        "primary:Downloads",
                        "document",
                        "primary:Downloads",
                        "folder1",
                        "folder2",
                        "file.txt"
                    )
                }

                whenever(contentUriMock.toString()).thenReturn(uriString)
                whenever(Uri.parse(uriString)).thenReturn(contentUriMock)

                val stalledIssue = StalledIssue(
                    syncId = 10L,
                    nodeIds = listOf(NodeId(13L)),
                    localPaths = listOf(uriString),
                    issueType = StallIssueType.DownloadIssue,
                    conflictName = "uri segments test",
                    nodeNames = emptyList(),
                    id = "1_3_0"
                )
                val detailedInfo = StalledIssueDetailedInfo("URI Title", "URI Description")
                val resolutionActions = listOf<StalledIssueResolutionAction>()

                whenever(stalledIssueDetailInfoMapper(stalledIssue)).thenReturn(detailedInfo)
                whenever(stalledIssueResolutionActionMapper(StallIssueType.DownloadIssue, true))
                    .thenReturn(resolutionActions)

                val result = underTest(stalledIssue, emptyList())

                assertThat(result.displayedName).isEqualTo("file.txt")
                assertThat(result.displayedPath).isEqualTo("primary:Downloads/folder1/folder2")
            }
        }

    @Test
    fun `test that areAllNodesFolders is false when nodes contain mixed types`() = runTest {
        val stalledIssue = StalledIssue(
            syncId = 9L,
            nodeIds = listOf(NodeId(11L), NodeId(12L)),
            localPaths = listOf("/storage/emulated/0/Mixed"),
            issueType = StallIssueType.UploadIssue,
            conflictName = "mixed nodes conflict",
            nodeNames = listOf("folder", "file.txt"),
            id = "1_3_0"
        )
        val folderNode: FolderNode = mock()
        val fileNode: FileNode = mock {
            on { type } doReturn PdfFileTypeInfo
        }
        val detailedInfo = StalledIssueDetailedInfo("Mixed Title", "Mixed Description")
        val resolutionActions = listOf<StalledIssueResolutionAction>()

        whenever(stalledIssueDetailInfoMapper(stalledIssue)).thenReturn(detailedInfo)
        whenever(stalledIssueResolutionActionMapper(StallIssueType.UploadIssue, false))
            .thenReturn(resolutionActions)

        val result = underTest(stalledIssue, listOf(folderNode, fileNode))

        // Verify that areAllNodesFolders = false was passed to the mapper
        verify(stalledIssueResolutionActionMapper)(StallIssueType.UploadIssue, false)
    }

    @Test
    fun `test that StalledIssueUiItem with empty localPaths uses displayedPath`() {
        val stalledIssueUiItem = StalledIssueUiItem(
            syncId = 1L,
            nodeIds = listOf(NodeId(3L)),
            localPaths = emptyList(),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
            icon = 0,
            detailedInfo = StalledIssueDetailedInfo("", ""),
            actions = emptyList(),
            displayedName = "Camera",
            displayedPath = "/storage/emulated/0/DCIM",
            id = "1_3_0"
        )

        val result = underTest(stalledIssueUiItem)

        assertThat(result.localPaths).isEqualTo(listOf("/storage/emulated/0/DCIM"))
    }

    @Test
    fun `test that mapping handles single level path correctly`() = runTest {
        val stalledIssue = StalledIssue(
            syncId = 6L,
            nodeIds = listOf(NodeId(8L)),
            localPaths = listOf("/storage/emulated/0/Documents"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "single level conflict",
            nodeNames = listOf("Documents/file.txt"),
            id = "1_3_0"
        )
        val detailedInfo = StalledIssueDetailedInfo("Single Title", "Single Description")
        val resolutionActions = listOf<StalledIssueResolutionAction>()

        whenever(stalledIssueDetailInfoMapper(stalledIssue)).thenReturn(detailedInfo)
        whenever(stalledIssueResolutionActionMapper(StallIssueType.DownloadIssue, true))
            .thenReturn(resolutionActions)

        val result = underTest(stalledIssue, emptyList())

        assertThat(result.displayedName).isEqualTo("file.txt")
        assertThat(result.displayedPath).isEqualTo("Documents")
    }
}
