package mega.privacy.android.feature.sync.ui.mapper.solvedissue

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.ResolutionActionTypeToResolutionNameMapper
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SolvedIssueItemMapperTest {

    private val fileTypeIconMapper: FileTypeIconMapper = mock()
    private val resolutionActionTypeToResolutionNameMapper: ResolutionActionTypeToResolutionNameMapper =
        mock()

    private lateinit var underTest: SolvedIssueItemMapper

    @BeforeEach
    fun setUp() {
        // Reset mocks before each test
        reset(
            fileTypeIconMapper,
            resolutionActionTypeToResolutionNameMapper
        )

        underTest = SolvedIssueItemMapper(
            fileTypeIconMapper = fileTypeIconMapper,
            resolutionActionTypeToResolutionNameMapper = resolutionActionTypeToResolutionNameMapper,
        )
    }

    @Test
    fun `test that SolvedIssue is mapped to SolvedIssueUiItem with folder node`() = runTest {

        val solvedIssue = SolvedIssue(
            syncId = 1L,
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            resolutionExplanation = "RENAME_ALL_ITEMS"
        )
        val folderNode: FolderNode = mock {
            on { name } doReturn "Camera"
        }
        val expectedResolutionName = "Rename all items"

        whenever(resolutionActionTypeToResolutionNameMapper(StalledIssueResolutionActionType.RENAME_ALL_ITEMS))
            .thenReturn(expectedResolutionName)

        val result = underTest(solvedIssue, listOf(folderNode))
        assertThat(result.nodeIds).isEqualTo(listOf(NodeId(3L)))
        assertThat(result.nodeNames).isEqualTo(listOf("Camera"))
        assertThat(result.localPaths).isEqualTo(listOf("/storage/emulated/0/DCIM"))
        assertThat(result.resolutionExplanation).isEqualTo(expectedResolutionName)
        assertThat(result.icon).isEqualTo(iconPackR.drawable.ic_folder_medium_solid)

        verify(resolutionActionTypeToResolutionNameMapper).invoke(StalledIssueResolutionActionType.RENAME_ALL_ITEMS)
    }

    @Test
    fun `test that SolvedIssue is mapped to SolvedIssueUiItem with file node`() = runTest {

        val solvedIssue = SolvedIssue(
            syncId = 2L,
            nodeIds = listOf(NodeId(4L)),
            localPaths = listOf("/storage/emulated/0/Documents"),
            resolutionExplanation = "MERGE_FOLDERS"
        )
        val fileTypeInfo = PdfFileTypeInfo
        val fileNode: FileNode = mock {
            on { name } doReturn "document.pdf"
            on { type } doReturn fileTypeInfo
        }
        val expectedResolutionName = "Merge folders"
        val expectedIcon = 12345

        whenever(resolutionActionTypeToResolutionNameMapper(StalledIssueResolutionActionType.MERGE_FOLDERS))
            .thenReturn(expectedResolutionName)
        whenever(fileTypeIconMapper("pdf")).thenReturn(expectedIcon)

        val result = underTest(solvedIssue, listOf(fileNode))
        assertThat(result.nodeIds).isEqualTo(listOf(NodeId(4L)))
        assertThat(result.nodeNames).isEqualTo(listOf("document.pdf"))
        assertThat(result.localPaths).isEqualTo(listOf("/storage/emulated/0/Documents"))
        assertThat(result.resolutionExplanation).isEqualTo(expectedResolutionName)
        assertThat(result.icon).isEqualTo(expectedIcon)

        verify(resolutionActionTypeToResolutionNameMapper).invoke(StalledIssueResolutionActionType.MERGE_FOLDERS)
        verify(fileTypeIconMapper).invoke("pdf")
    }

    @Test
    fun `test that SolvedIssue is mapped to SolvedIssueUiItem with tree URI path`() = runTest {
        mockStatic(Uri::class.java).use { uriMock ->
            val uriString =
                "content://com.android.externalstorage.documents/tree/primary%3ASync%2FsomeFolder"
            val expectedParsedPath = "primary:Sync"
            val contentUriMock: Uri = mock {
                on { scheme } doReturn "content"
                on { pathSegments } doReturn listOf(
                    "tree",
                    "primary:Sync",
                    "document",
                    "primary:Sync",
                    "someFolder",
                )
            }

            whenever(contentUriMock.toString()).thenReturn(uriString)
            whenever(Uri.parse(uriString)).thenReturn(contentUriMock)

            val solvedIssue = SolvedIssue(
                syncId = 3L,
                nodeIds = listOf(NodeId(5L)),
                localPaths = listOf(uriString),
                resolutionExplanation = "CHOOSE_LOCAL_FILE"
            )
            val expectedResolutionName = "Choose local file"

            whenever(resolutionActionTypeToResolutionNameMapper(StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE))
                .thenReturn(expectedResolutionName)

            val result = underTest(solvedIssue, emptyList())

            assertThat(result.nodeIds).isEqualTo(listOf(NodeId(5L)))
            assertThat(result.nodeNames).isEqualTo(listOf("someFolder"))
            // The mapper should parse the URI and extract the path segments correctly
            assertThat(result.localPaths).isEqualTo(listOf(expectedParsedPath))
            assertThat(result.resolutionExplanation).isEqualTo(expectedResolutionName)
            assertThat(result.icon).isEqualTo(0) // Resource ID is 0 in unit tests

            verify(resolutionActionTypeToResolutionNameMapper).invoke(
                StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE
            )
        }
    }

    @Test
    fun `test that SolvedIssue is mapped to SolvedIssueUiItem with multiple nodes`() = runTest {

        val solvedIssue = SolvedIssue(
            syncId = 4L,
            nodeIds = listOf(NodeId(6L), NodeId(7L)),
            localPaths = listOf("/path1", "/path2"),
            resolutionExplanation = "CHOOSE_REMOTE_FILE"
        )
        val folderNode1: FolderNode = mock {
            on { name } doReturn "Folder1"
        }
        val folderNode2: FolderNode = mock {
            on { name } doReturn "Folder2"
        }
        val expectedResolutionName = "Choose remote file"

        whenever(resolutionActionTypeToResolutionNameMapper(StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE))
            .thenReturn(expectedResolutionName)

        val result = underTest(solvedIssue, listOf(folderNode1, folderNode2))

        assertThat(result.nodeIds).isEqualTo(listOf(NodeId(6L), NodeId(7L)))
        assertThat(result.nodeNames).isEqualTo(listOf("Folder1", "Folder2"))
        assertThat(result.localPaths).isEqualTo(listOf("/path1", "/path2"))
        assertThat(result.resolutionExplanation).isEqualTo(expectedResolutionName)
        // In unit tests, resource IDs might be resolved, so we just check that it's not 0
        assertThat(result.icon).isNotEqualTo(0)

        verify(resolutionActionTypeToResolutionNameMapper).invoke(StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE)
    }

    @Test
    fun `test that SolvedIssue is mapped to SolvedIssueUiItem with file extension from local path`() =
        runTest {

            val solvedIssue = SolvedIssue(
                syncId = 5L,
                nodeIds = listOf(NodeId(8L)),
                localPaths = listOf("/storage/emulated/0/Documents/file.txt"),
                resolutionExplanation = "CHOOSE_LATEST_MODIFIED_TIME"
            )
            val expectedResolutionName = "Choose the one with the latest modified time"
            val expectedIcon = 67890

            whenever(resolutionActionTypeToResolutionNameMapper(StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME))
                .thenReturn(expectedResolutionName)
            whenever(fileTypeIconMapper("txt")).thenReturn(expectedIcon)

            val result = underTest(solvedIssue, emptyList())

            assertThat(result.nodeIds).isEqualTo(listOf(NodeId(8L)))
            assertThat(result.nodeNames).isEqualTo(listOf("file.txt"))
            assertThat(result.localPaths).isEqualTo(listOf("/storage/emulated/0/Documents"))
            assertThat(result.resolutionExplanation).isEqualTo(expectedResolutionName)
            assertThat(result.icon).isEqualTo(expectedIcon)

            verify(resolutionActionTypeToResolutionNameMapper).invoke(
                StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME
            )
            verify(fileTypeIconMapper).invoke("txt")
        }

    @Test
    fun `test that SolvedIssue is mapped to SolvedIssueUiItem with unknown resolution action type`() =
        runTest {

            val solvedIssue = SolvedIssue(
                syncId = 6L,
                nodeIds = listOf(NodeId(9L)),
                localPaths = listOf("/storage/emulated/0/Unknown"),
                resolutionExplanation = "UNKNOWN_ACTION"
            )
            val expectedResolutionName = ""

            whenever(resolutionActionTypeToResolutionNameMapper(StalledIssueResolutionActionType.UNKNOWN))
                .thenReturn(expectedResolutionName)

            val result = underTest(solvedIssue, emptyList())

            assertThat(result.nodeIds).isEqualTo(listOf(NodeId(9L)))
            assertThat(result.nodeNames).isEqualTo(listOf("Unknown"))
            assertThat(result.localPaths).isEqualTo(listOf("/storage/emulated/0"))
            assertThat(result.resolutionExplanation).isEqualTo(expectedResolutionName)
            assertThat(result.icon).isEqualTo(iconPackR.drawable.ic_generic_medium_solid)

            verify(resolutionActionTypeToResolutionNameMapper).invoke(
                StalledIssueResolutionActionType.UNKNOWN
            )
        }

    @Test
    fun `test that SolvedIssue is mapped to SolvedIssueUiItem with empty nodes list`() = runTest {

        val solvedIssue = SolvedIssue(
            syncId = 7L,
            nodeIds = listOf(NodeId(10L)),
            localPaths = listOf("/storage/emulated/0/Empty"),
            resolutionExplanation = "RENAME_ALL_ITEMS"
        )
        val expectedResolutionName = "Rename all items"

        whenever(resolutionActionTypeToResolutionNameMapper(StalledIssueResolutionActionType.RENAME_ALL_ITEMS))
            .thenReturn(expectedResolutionName)

        val result = underTest(solvedIssue, emptyList())

        assertThat(result.nodeIds).isEqualTo(listOf(NodeId(10L)))
        assertThat(result.nodeNames).isEqualTo(listOf("Empty"))
        assertThat(result.localPaths).isEqualTo(listOf("/storage/emulated/0"))
        assertThat(result.resolutionExplanation).isEqualTo(expectedResolutionName)
        assertThat(result.icon).isEqualTo(iconPackR.drawable.ic_generic_medium_solid)

        verify(resolutionActionTypeToResolutionNameMapper).invoke(StalledIssueResolutionActionType.RENAME_ALL_ITEMS)
    }

    @Test
    fun `test that SolvedIssue is mapped to SolvedIssueUiItem with file node and extension`() =
        runTest {

            val solvedIssue = SolvedIssue(
                syncId = 9L,
                nodeIds = listOf(NodeId(12L)),
                localPaths = listOf("/storage/emulated/0/Documents"),
                resolutionExplanation = "CHOOSE_LOCAL_FILE"
            )
            val fileTypeInfo = mock<mega.privacy.android.domain.entity.UnknownFileTypeInfo> {
                on { extension } doReturn "docx"
            }
            val fileNode: FileNode = mock {
                on { name } doReturn "document.docx"
                on { type } doReturn fileTypeInfo
            }
            val expectedResolutionName = "Choose local file"
            val expectedIcon = 11111

            whenever(resolutionActionTypeToResolutionNameMapper(StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE))
                .thenReturn(expectedResolutionName)
            whenever(fileTypeIconMapper("docx")).thenReturn(expectedIcon)

            val result = underTest(solvedIssue, listOf(fileNode))

            assertThat(result.nodeIds).isEqualTo(listOf(NodeId(12L)))
            assertThat(result.nodeNames).isEqualTo(listOf("document.docx"))
            assertThat(result.localPaths).isEqualTo(listOf("/storage/emulated/0/Documents"))
            assertThat(result.resolutionExplanation).isEqualTo(expectedResolutionName)
            assertThat(result.icon).isEqualTo(expectedIcon)

            verify(resolutionActionTypeToResolutionNameMapper).invoke(
                StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE
            )
            verify(fileTypeIconMapper).invoke("docx")
        }

    @ParameterizedTest(name = "Test resolution action type {0} returns correct mapping")
    @MethodSource("resolutionActionTypeProvider")
    fun `test that different resolution action types are mapped correctly`(
        resolutionActionType: StalledIssueResolutionActionType,
        expectedResolutionName: String,
    ) = runTest {

        val solvedIssue = SolvedIssue(
            syncId = 10L,
            nodeIds = listOf(NodeId(13L)),
            localPaths = listOf("/storage/emulated/0/Test"),
            resolutionExplanation = resolutionActionType.name
        )

        whenever(resolutionActionTypeToResolutionNameMapper(resolutionActionType))
            .thenReturn(expectedResolutionName)


        val result = underTest(solvedIssue, emptyList())

        assertThat(result.resolutionExplanation).isEqualTo(expectedResolutionName)
        verify(resolutionActionTypeToResolutionNameMapper).invoke(resolutionActionType)
    }

    @Test
    fun `test that SolvedIssue is mapped with node name and parsed content URI path`() = runTest {
        mockStatic(Uri::class.java).use { uriMock ->
            val uriString =
                "content://com.android.externalstorage.documents/tree/primary%3ASync%2Fdocument.pdf/"
            val expectedParsedPath = "primary:Sync"
            val contentUriMock: Uri = mock {
                on { scheme } doReturn "content"
                on { pathSegments } doReturn listOf(
                    "tree",
                    "primary:Sync",
                    "document",
                    "primary:Sync",
                    "document.pdf",
                )
            }

            whenever(Uri.parse(uriString)).thenReturn(contentUriMock)

            val solvedIssue = SolvedIssue(
                syncId = 8L,
                nodeIds = listOf(NodeId(11L)),
                localPaths = listOf(uriString),
                resolutionExplanation = "CHOOSE_LOCAL_FILE"
            )

            val fileTypeInfo = PdfFileTypeInfo
            val fileNode: FileNode = mock {
                on { name } doReturn "document.pdf"
                on { type } doReturn fileTypeInfo
            }

            val expectedResolutionName = "Choose local file"
            val expectedIcon = 54321

            whenever(resolutionActionTypeToResolutionNameMapper(StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE))
                .thenReturn(expectedResolutionName)
            whenever(fileTypeIconMapper("pdf")).thenReturn(expectedIcon)

            val result = underTest(solvedIssue, listOf(fileNode))

            assertThat(result.nodeIds).isEqualTo(listOf(NodeId(11L)))
            assertThat(result.nodeNames).isEqualTo(listOf("document.pdf")) // Name from node
            assertThat(result.localPaths).isEqualTo(listOf(expectedParsedPath)) // Parsed from URI
            assertThat(result.resolutionExplanation).isEqualTo(expectedResolutionName)
            assertThat(result.icon).isEqualTo(expectedIcon)

            verify(resolutionActionTypeToResolutionNameMapper).invoke(
                StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE
            )
            verify(fileTypeIconMapper).invoke("pdf")
        }
    }

    private fun resolutionActionTypeProvider() = Stream.of(
        Arguments.of(
            StalledIssueResolutionActionType.RENAME_ALL_ITEMS,
            "Rename all items"
        ),
        Arguments.of(
            StalledIssueResolutionActionType.MERGE_FOLDERS,
            "Merge folders"
        ),
        Arguments.of(
            StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE,
            "Choose local file"
        ),
        Arguments.of(
            StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE,
            "Choose remote file"
        ),
        Arguments.of(
            StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME,
            "Choose the one with the latest modified time"
        ),
        Arguments.of(
            StalledIssueResolutionActionType.UNKNOWN,
            ""
        )
    )
}
