package mega.privacy.android.domain.usecase.downloads

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadNodeUseCaseTest {

    private val fileSystemRepository: FileSystemRepository = mock()
    private val nodeRepository: NodeRepository = mock()
    private val transferRepository: TransferRepository = mock()
    private val fileNode: TypedFileNode = mock()
    private val folderNode: TypedFolderNode = mock()

    private lateinit var underTest: DownloadNodeUseCase

    @BeforeAll
    fun setup() {
        underTest = DownloadNodeUseCase(
            nodeRepository, transferRepository, fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            fileSystemRepository, nodeRepository, transferRepository,
            fileNode, folderNode,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that destination directory is created when the node is a folder`() = runTest {
        whenever(nodeRepository.getNodeById(nodeId)).thenReturn(folderNode)
        underTest(nodeId, DESTINATION_PATH_FOLDER, null, false).test {
            verify(fileSystemRepository).createDirectory(DESTINATION_PATH_FOLDER)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that destination parent directory is created when the node is a file`() = runTest {
        whenever(nodeRepository.getNodeById(nodeId)).thenReturn(fileNode)
        whenever(fileSystemRepository.getParent(DESTINATION_PATH_FILE))
            .thenReturn(DESTINATION_PATH_FOLDER)
        underTest(nodeId, DESTINATION_PATH_FILE, null, false).test {
            verify(fileSystemRepository).createDirectory(DESTINATION_PATH_FOLDER)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that repository result is returned`() = runTest {
        val expected: Flow<Transfer> = mock()
        whenever(transferRepository.startDownload(nodeId, DESTINATION_PATH_FOLDER, null, false))
            .thenReturn(expected)
        underTest(nodeId, DESTINATION_PATH_FOLDER, null, false).test {
            verify(transferRepository).startDownload(nodeId, DESTINATION_PATH_FOLDER, null, false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "priority: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that repository is called with the proper priority`(priority: Boolean) = runTest {
        underTest(nodeId, DESTINATION_PATH_FOLDER, null, priority).test {
            verify(transferRepository).startDownload(
                nodeId,
                DESTINATION_PATH_FOLDER,
                null,
                priority
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "appdata: \"{0}\"")
    @ValueSource(strings = ["test1", "", " ", "some space"])
    fun `test that repository is called with the proper appData`(appData: String?) = runTest {
        underTest(nodeId, DESTINATION_PATH_FOLDER, appData, false).test {
            verify(transferRepository).startDownload(
                nodeId,
                DESTINATION_PATH_FOLDER,
                appData,
                false
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    companion object {
        private val nodeId = NodeId(1L)
        private const val DESTINATION_PATH_FOLDER = "root/parent/destination"
        private const val DESTINATION_PATH_FILE = "root/parent/destination/file.txt"
    }
}