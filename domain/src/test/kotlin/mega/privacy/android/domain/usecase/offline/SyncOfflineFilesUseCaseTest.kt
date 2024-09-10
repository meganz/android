package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncOfflineFilesUseCaseTest {

    private lateinit var underTest: SyncOfflineFilesUseCase

    private val clearOfflineUseCase: ClearOfflineUseCase = mock()
    private val getOfflineFilesUseCase: GetOfflineFilesUseCase = mock()
    private val nodeRepository: NodeRepository = mock()
    private val hasOfflineFilesUseCase: HasOfflineFilesUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = SyncOfflineFilesUseCase(
            clearOfflineUseCase = clearOfflineUseCase,
            getOfflineFilesUseCase = getOfflineFilesUseCase,
            nodeRepository = nodeRepository,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(clearOfflineUseCase, getOfflineFilesUseCase, nodeRepository, hasOfflineFilesUseCase)
    }

    @Test
    fun `test that node information is removed when file doesn't exist`() = runTest {
        whenever(hasOfflineFilesUseCase()) doReturn true
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
    fun `test that getOfflineFilesUseCase is not invoked when offline nodes doesn't exist`() =
        runTest {
            whenever(hasOfflineFilesUseCase()) doReturn true

            whenever(nodeRepository.getAllOfflineNodes()).thenReturn(emptyList())

            underTest()

            verifyNoInteractions(getOfflineFilesUseCase)
        }

    @Test
    fun `test that node information is removed when folder is empty`() = runTest {
        whenever(hasOfflineFilesUseCase()) doReturn true

        val offlineNodes = listOf(mock<OtherOfflineNodeInformation> {
            on { isFolder } doReturn true
            on { handle } doReturn "123"
            on { id } doReturn 1
        })
        whenever(nodeRepository.getAllOfflineNodes()).thenReturn(offlineNodes)
        val offlineFile = mock<File> {
            on { exists() } doReturn true
            on { isDirectory } doReturn true
        }
        val fileMap = mapOf(123 to offlineFile)
        whenever(getOfflineFilesUseCase(offlineNodes)).thenReturn(fileMap)

        underTest()

        verify(nodeRepository).removeOfflineNodeByIds(listOf(123))
    }

    @Test
    fun `test that child folders are deleted first`() = runTest {
        whenever(hasOfflineFilesUseCase()) doReturn true

        val offlineNodes = listOf(mock<OtherOfflineNodeInformation> {
            on { isFolder } doReturn true
            on { handle } doReturn "123"
            on { id } doReturn 1
        }, mock<OtherOfflineNodeInformation> {
            on { isFolder } doReturn true
            on { handle } doReturn "234"
            on { id } doReturn 2
        })
        whenever(nodeRepository.getAllOfflineNodes()).thenReturn(offlineNodes)
        val offlineFile = mock<File> {
            on { exists() } doReturn true
            on { isDirectory } doReturn true
        }
        val offlineFile2 = mock<File> {
            on { exists() } doReturn true
            on { isDirectory } doReturn true
        }
        val fileMap = mapOf(1 to offlineFile, 2 to offlineFile2)
        whenever(getOfflineFilesUseCase(offlineNodes)).thenReturn(fileMap)

        underTest()

        verify(nodeRepository).removeOfflineNodeByIds(listOf(2, 1))
    }

    @Test
    fun `test that offline is cleared when offline directory doesn't exist but database entries exists`() =
        runTest {
            whenever(hasOfflineFilesUseCase()) doReturn false

            val offlineNodes = listOf(mock<OtherOfflineNodeInformation> {
                on { isFolder } doReturn true
                on { handle } doReturn "123"
                on { id } doReturn 1
            })
            whenever(nodeRepository.getAllOfflineNodes()).thenReturn(offlineNodes)

            underTest()

            verify(clearOfflineUseCase).invoke()
        }

    @Test
    fun `test that offline is cleared when offline directory exists but database entries doesn't exists`() =
        runTest {
            whenever(hasOfflineFilesUseCase()) doReturn true

            whenever(nodeRepository.getAllOfflineNodes()).thenReturn(emptyList())

            underTest()

            verify(clearOfflineUseCase).invoke()
        }
}