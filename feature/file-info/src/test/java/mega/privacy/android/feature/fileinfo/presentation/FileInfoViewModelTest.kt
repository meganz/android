package mega.privacy.android.feature.fileinfo.presentation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileInfoViewModelTest {

    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val monitorNodeUpdatesById: MonitorNodeUpdatesById = mock()
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase = mock()
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase = mock()
    private val getNodeAccessPermission: GetNodeAccessPermission = mock()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()

    private val fileTypeInfo = UnknownFileTypeInfo(mimeType = "image/heic", extension = "heic")

    private lateinit var underTest: FileInfoViewModel

    private fun initViewModel(nodeHandle: Long = NODE_HANDLE) {
        underTest = FileInfoViewModel(
            getNodeByIdUseCase = getNodeByIdUseCase,
            monitorNodeUpdatesById = monitorNodeUpdatesById,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            fileTypeIconMapper = fileTypeIconMapper,
            nodeHandle = nodeHandle,
        )
    }

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            getNodeByIdUseCase,
            monitorNodeUpdatesById,
            isNodeInRubbishBinUseCase,
            isNodeInBackupsUseCase,
            getNodeAccessPermission,
            fileTypeIconMapper,
        )
        whenever(monitorNodeUpdatesById(any())).thenReturn(emptyFlow())
        whenever(fileTypeIconMapper(any(), any())).thenReturn(FILE_ICON_RES)
    }

    private fun mockFileNode(
        name: String = "file.txt",
        size: Long = 1024L,
        creationTime: Long = 100L,
        modificationTime: Long = 200L,
        description: String? = "a description",
        tags: List<String>? = listOf("marketing", "2026"),
        isTakenDown: Boolean = false,
    ): TypedFileNode = mock {
        on { id } doReturn NodeId(NODE_HANDLE)
        on { this.name } doReturn name
        on { this.size } doReturn size
        on { this.creationTime } doReturn creationTime
        on { this.modificationTime } doReturn modificationTime
        on { this.description } doReturn description
        on { this.tags } doReturn tags
        on { this.isTakenDown } doReturn isTakenDown
        on { this.type } doReturn fileTypeInfo
    }

    private fun mockFolderNode(
        name: String = "folder",
        creationTime: Long = 100L,
    ): TypedFolderNode = mock {
        on { id } doReturn NodeId(NODE_HANDLE)
        on { this.name } doReturn name
        on { this.creationTime } doReturn creationTime
        on { description } doReturn null
        on { tags } doReturn null
        on { isTakenDown } doReturn false
    }

    @Test
    fun `test that init loads file node metadata into state`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(isLoading).isFalse()
            assertThat(title).isEqualTo("file.txt")
            assertThat(isFile).isTrue()
            assertThat(iconRes).isEqualTo(FILE_ICON_RES)
            assertThat(thumbnailData).isEqualTo(ThumbnailRequest(NodeId(NODE_HANDLE)))
            assertThat(fileTypeExtension).isEqualTo("heic")
            assertThat(sizeInBytes).isEqualTo(1024L)
            assertThat(creationTime).isEqualTo(100L)
            assertThat(modificationTime).isEqualTo(200L)
            assertThat(descriptionText).isEqualTo("a description")
            assertThat(tags).containsExactly("marketing", "2026")
            assertThat(isTakenDown).isFalse()
            assertThat(accessPermission).isEqualTo(AccessPermission.OWNER)
        }
    }

    @Test
    fun `test that init sets isFile false and no modification time for a folder`() = runTest {
        val node = mockFolderNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(isFile).isFalse()
            assertThat(title).isEqualTo("folder")
            assertThat(fileTypeExtension).isNull()
            assertThat(thumbnailData).isNull()
            assertThat(iconRes).isNotNull()
            assertThat(sizeInBytes).isEqualTo(0L)
            assertThat(modificationTime).isNull()
            assertThat(descriptionText).isEmpty()
            assertThat(tags).isEmpty()
        }
    }

    @Test
    fun `test that init stops loading when node is not found`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(isLoading).isFalse()
            assertThat(title).isEmpty()
        }
    }

    @Test
    fun `test that rubbish and backups flags are set from use cases`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(isNodeInRubbishBinUseCase(NodeId(NODE_HANDLE))).thenReturn(true)
        whenever(isNodeInBackupsUseCase(NODE_HANDLE)).thenReturn(true)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(isNodeInRubbish).isTrue()
            assertThat(isNodeInBackups).isTrue()
        }
    }

    @Test
    fun `test that access permission defaults to UNKNOWN when use case returns null`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.accessPermission).isEqualTo(AccessPermission.UNKNOWN)
    }

    @Test
    fun `test that a node update re-fetches node info`() = runTest {
        val oldNode = mockFileNode(name = "old.txt")
        val newNode = mockFileNode(name = "new.txt")
        whenever(monitorNodeUpdatesById(NodeId(NODE_HANDLE)))
            .thenReturn(flowOf(listOf(NodeChanges.Name)))
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE)))
            .thenReturn(oldNode)
            .thenReturn(newNode)

        initViewModel()
        advanceUntilIdle()

        verify(getNodeByIdUseCase, times(2)).invoke(NodeId(NODE_HANDLE))
        assertThat(underTest.uiState.value.title).isEqualTo("new.txt")
    }

    private companion object {
        const val NODE_HANDLE = 99113034474275L
        const val FILE_ICON_RES = 12345
    }
}
