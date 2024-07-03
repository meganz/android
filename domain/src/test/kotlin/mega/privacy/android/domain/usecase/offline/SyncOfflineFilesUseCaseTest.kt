package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncOfflineFilesUseCaseTest {

    private lateinit var underTest: SyncOfflineFilesUseCase

    private val clearOfflineUseCase: ClearOfflineUseCase = mock()
    private val removeOfflineNodesUseCase: RemoveOfflineNodesUseCase = mock()
    private val getOfflineFilesUseCase: GetOfflineFilesUseCase = mock()
    private val nodeRepository: NodeRepository = mock()
    private val fileSystemRepository: FileSystemRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = SyncOfflineFilesUseCase(
            clearOfflineUseCase,
            removeOfflineNodesUseCase,
            getOfflineFilesUseCase,
            nodeRepository,
            fileSystemRepository
        )
    }

    @Test
    fun `test that node information is removed if file doesn't exist`() = runTest {
        val offlineFolder = mock<File> {
            on { exists() } doReturn true
        }
        whenever(fileSystemRepository.getOfflineFolder()).thenReturn(offlineFolder)
        val offlineNodes = listOf(mock<OtherOfflineNodeInformation>())
        whenever(nodeRepository.getAllOfflineNodes()).thenReturn(offlineNodes)
        val offlineFile = mock<File> {
            on { exists() } doReturn false
        }
        val fileMap = mapOf(123 to offlineFile)
        whenever(getOfflineFilesUseCase(offlineNodes)).thenReturn(fileMap)

        underTest()

        verify(nodeRepository).removeOfflineNodeByIds(listOf(123))
    }


    @Test
    fun `test that node information is removed if folder is empty`() = runTest {
        val offlineFolder = mock<File> {
            on { exists() } doReturn true
        }
        whenever(fileSystemRepository.getOfflineFolder()).thenReturn(offlineFolder)
        val offlineNodes = listOf(mock<OtherOfflineNodeInformation> {
            on { isFolder } doReturn true
            on { handle } doReturn "123"
            on { id } doReturn 1
        })
        whenever(nodeRepository.getAllOfflineNodes()).thenReturn(offlineNodes)
        val offlineFile = mock<File> {
            on { exists() } doReturn true
        }
        val fileMap = mapOf(123 to offlineFile)
        whenever(getOfflineFilesUseCase(offlineNodes)).thenReturn(fileMap)
        whenever(nodeRepository.getOfflineNodesByParentId(1)).thenReturn(emptyList())

        underTest()

        verify(removeOfflineNodesUseCase).invoke(listOf(NodeId(123)))
    }

    @Test
    fun `test that offline database is cleared if offline directory doesn't exist but entries exists`() =
        runTest {
            val offlineFolder = mock<File> {
                on { exists() } doReturn false
            }
            whenever(fileSystemRepository.getOfflineFolder()).thenReturn(offlineFolder)
            val offlineNodes = listOf(mock<OtherOfflineNodeInformation> {
                on { isFolder } doReturn true
                on { handle } doReturn "123"
                on { id } doReturn 1
            })
            whenever(nodeRepository.getAllOfflineNodes()).thenReturn(offlineNodes)

            underTest()

            verify(clearOfflineUseCase).invoke()
        }
}