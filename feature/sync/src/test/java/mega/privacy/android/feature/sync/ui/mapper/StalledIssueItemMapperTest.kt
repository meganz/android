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
import org.mockito.kotlin.any
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
        verify(stalledIssueResolutionActionMapper).invoke(StallIssueType.DownloadIssue, true)
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
        verify(stalledIssueResolutionActionMapper).invoke(StallIssueType.UploadIssue, false)
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
            whenever(Uri.parse(uriString)).thenReturn(contentUriMock) // Ensure this mock is effective
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
            // When nodes is empty, areAllNodesFolders will be false
            whenever(stalledIssueResolutionActionMapper(StallIssueType.DownloadIssue, false))
                .thenReturn(resolutionActions)

            val result = underTest(stalledIssue, emptyList())

            assertThat(result.icon).isEqualTo(iconPackR.drawable.ic_generic_medium_solid)
            assertThat(result.displayedName).isEqualTo("file.txt")
            assertThat(result.displayedPath).isEqualTo("primary:path")
            verify(stalledIssueResolutionActionMapper).invoke(StallIssueType.DownloadIssue, false)
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
        // When nodes is empty, areAllNodesFolders will be false
        whenever(stalledIssueResolutionActionMapper(StallIssueType.UploadIssue, false))
            .thenReturn(resolutionActions)
        whenever(fileTypeIconMapper("jpg")).thenReturn(expectedIcon)

        val result = underTest(stalledIssue, emptyList())

        assertThat(result.icon).isEqualTo(expectedIcon)
        verify(fileTypeIconMapper)("jpg")
        verify(stalledIssueResolutionActionMapper).invoke(StallIssueType.UploadIssue, false)
    }

    @Test
    fun `test areAllNodesFolders is true when all nodes are folders`() = runTest {
        val stalledIssue = StalledIssue(
            syncId = 1L,
            nodeIds = listOf(NodeId(3L), NodeId(4L)),
            localPaths = emptyList(),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "folder conflict",
            nodeNames = listOf("FolderA/sub", "FolderB"),
            id = "1_3_0"
        )
        val folderNode1: FolderNode = mock()
        val folderNode2: FolderNode = mock()
        val detailedInfo = StalledIssueDetailedInfo("Title", "Description")
        val resolutionActions = listOf<StalledIssueResolutionAction>()

        whenever(stalledIssueDetailInfoMapper(stalledIssue)).thenReturn(detailedInfo)
        whenever(stalledIssueResolutionActionMapper(StallIssueType.DownloadIssue, true))
            .thenReturn(resolutionActions)

        underTest(stalledIssue, listOf(folderNode1, folderNode2))

        verify(stalledIssueResolutionActionMapper).invoke(StallIssueType.DownloadIssue, true)
    }

    @Test
    fun `test areAllNodesFolders is false when nodes list is mixed with files and folders`() =
        runTest {
            val stalledIssue = StalledIssue(
                syncId = 2L,
                nodeIds = listOf(NodeId(5L), NodeId(6L)),
                localPaths = emptyList(),
                issueType = StallIssueType.UploadIssue,
                conflictName = "mixed conflict",
                nodeNames = listOf("FolderC/file.txt", "AnotherFolder"),
                id = "1_3_0"
            )
            val folderNode: FolderNode = mock()
            val fileNode: FileNode =
                mock { on { type } doReturn PdfFileTypeInfo } // Mock type to avoid NPE
            val detailedInfo = StalledIssueDetailedInfo("Mixed Title", "Mixed Description")
            val resolutionActions = listOf<StalledIssueResolutionAction>()

            whenever(stalledIssueDetailInfoMapper(stalledIssue)).thenReturn(detailedInfo)
            whenever(stalledIssueResolutionActionMapper(StallIssueType.UploadIssue, false))
                .thenReturn(resolutionActions)
            whenever(
                fileTypeIconMapper(
                    any(),
                    any()
                )
            ).thenReturn(iconPackR.drawable.ic_generic_medium_solid)


            underTest(stalledIssue, listOf(folderNode, fileNode))

            verify(stalledIssueResolutionActionMapper).invoke(StallIssueType.UploadIssue, false)
        }

    @Test
    fun `test areAllNodesFolders is false when nodes list is empty`() = runTest {
        val stalledIssue = StalledIssue(
            syncId = 3L,
            nodeIds = emptyList(),
            localPaths = listOf("local/path/to/file.txt"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "empty nodes conflict",
            nodeNames = listOf("file.txt"),
            id = "1_3_0"
        )
        val detailedInfo = StalledIssueDetailedInfo("Empty Title", "Empty Description")
        val resolutionActions = listOf<StalledIssueResolutionAction>()

        whenever(stalledIssueDetailInfoMapper(stalledIssue)).thenReturn(detailedInfo)
        whenever(stalledIssueResolutionActionMapper(StallIssueType.DownloadIssue, false))
            .thenReturn(resolutionActions)
        whenever(
            fileTypeIconMapper(
                any(),
                any()
            )
        ).thenReturn(iconPackR.drawable.ic_generic_medium_solid)


        underTest(stalledIssue, emptyList())

        verify(stalledIssueResolutionActionMapper).invoke(StallIssueType.DownloadIssue, false)
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
        // When nodes list is empty, areAllNodesFolders will be false
        whenever(stalledIssueResolutionActionMapper(StallIssueType.DownloadIssue, false))
            .thenReturn(resolutionActions)
        whenever(
            fileTypeIconMapper(
                any(),
                any()
            )
        ).thenReturn(iconPackR.drawable.ic_generic_medium_solid)


        val result = underTest(stalledIssue, emptyList())

        assertThat(result.displayedName).isEqualTo("file.kt")
        assertThat(result.displayedPath).isEqualTo("Documents/Projects/Android")
        verify(stalledIssueResolutionActionMapper).invoke(StallIssueType.DownloadIssue, false)
    }

    @Test
    fun `test that Uri pathSegments drop logic works correctly with different segment counts`() =
        runTest {
            // Mocking android.net.Uri specifically for this test
            mockStatic(Uri::class.java).use { mockedUri ->
                val uriString =
                    "content://documents/tree/primary:Downloads/document/primary:Downloads/folder1/folder2/file.txt"
                val mockUri: Uri = mock {
                    on { scheme } doReturn "content" // Ensure scheme is mocked if used by toUri() or related logic
                    on { pathSegments } doReturn listOf(
                        "tree",
                        "primary:Downloads",
                        "document",
                        "primary:Downloads",
                        "folder1",
                        "folder2",
                        "file.txt"
                    )
                    on { toString() } doReturn uriString // Mock toString if it's called
                }
                mockedUri.`when`<Uri> { Uri.parse(uriString) }.thenReturn(mockUri)


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
                // When nodes list is empty, areAllNodesFolders will be false
                whenever(stalledIssueResolutionActionMapper(StallIssueType.DownloadIssue, false))
                    .thenReturn(resolutionActions)
                whenever(
                    fileTypeIconMapper(
                        any(),
                        any()
                    )
                ).thenReturn(iconPackR.drawable.ic_generic_medium_solid)


                val result = underTest(stalledIssue, emptyList())

                assertThat(result.displayedName).isEqualTo("file.txt")
                assertThat(result.displayedPath).isEqualTo("primary:Downloads/folder1/folder2")
                verify(stalledIssueResolutionActionMapper).invoke(
                    StallIssueType.DownloadIssue,
                    false
                )
            }
        }
}
