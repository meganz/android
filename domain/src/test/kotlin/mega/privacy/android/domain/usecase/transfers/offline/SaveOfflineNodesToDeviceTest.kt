package mega.privacy.android.domain.usecase.transfers.offline

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.offline.BackupsOfflineNodeInformation
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveOfflineNodesToDeviceTest {
    private lateinit var underTest: SaveOfflineNodesToDevice
    private val getOfflineNodeInformationUseCase: GetOfflineNodeInformationUseCase = mock()
    private val getOfflineFileUseCase: GetOfflineFileUseCase = mock()
    private val fileSystemRepository: FileSystemRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = SaveOfflineNodesToDevice(
            getOfflineNodeInformationUseCase,
            getOfflineFileUseCase,
            fileSystemRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getOfflineNodeInformationUseCase, getOfflineFileUseCase, fileSystemRepository)
    }

    // test that it's document uri and it invokes copyFilesToDocumentUri
    @Test
    fun `test that SaveOfflineNodeToDevice invokes copyFilesToDocumentUri in FileSystemRepository`() =
        runTest {
            val destinationUri = UriPath("destinationUri")
            val file = mock<File>()
            val offlineInfo = mock<BackupsOfflineNodeInformation>()
            val node = mock<TypedNode>()
            whenever(getOfflineNodeInformationUseCase(node)).thenReturn(offlineInfo)
            whenever(getOfflineFileUseCase(offlineInfo)).thenReturn(file)
            whenever(fileSystemRepository.isContentUri(destinationUri.value)).thenReturn(true)
            underTest(listOf(node), destinationUri)
            verify(fileSystemRepository).copyFilesToDocumentUri(file, destinationUri)
        }

    @Test
    fun `test that SaveOfflineNodeToDevice invokes copyFiles in FileSystemRepository when isFileUri returns true`() =
        runTest {
            val destinationUri = UriPath("destinationUri")
            val file = mock<File>()
            val offlineInfo = mock<BackupsOfflineNodeInformation>()
            val node = mock<TypedNode>()
            whenever(getOfflineNodeInformationUseCase(node)).thenReturn(offlineInfo)
            whenever(getOfflineFileUseCase(offlineInfo)).thenReturn(file)
            whenever(fileSystemRepository.isContentUri(destinationUri.value)).thenReturn(false)
            whenever(fileSystemRepository.isFileUri(destinationUri.value)).thenReturn(true)
            val destination = mock<File>()
            whenever(fileSystemRepository.getFileFromFileUri(destinationUri.value)).thenReturn(
                destination
            )
            underTest(listOf(node), destinationUri)
            verify(fileSystemRepository).copyFiles(file, destination)
        }

    @Test
    fun `test that SaveOfflineNodeToDevice invokes copyFiles in FileSystemRepository when getFileByPath returns different null`() =
        runTest {
            val destinationUri = UriPath("destinationUri")
            val file = mock<File>()
            val offlineInfo = mock<BackupsOfflineNodeInformation>()
            val node = mock<TypedNode>()
            whenever(getOfflineNodeInformationUseCase(node)).thenReturn(offlineInfo)
            whenever(getOfflineFileUseCase(offlineInfo)).thenReturn(file)
            whenever(fileSystemRepository.isContentUri(destinationUri.value)).thenReturn(false)
            whenever(fileSystemRepository.isFileUri(destinationUri.value)).thenReturn(false)
            whenever(fileSystemRepository.getFileByPath(destinationUri.value)).thenReturn(mock())
            val destination = mock<File>()
            whenever(fileSystemRepository.getFileByPath(destinationUri.value)).thenReturn(
                destination
            )
            underTest(listOf(node), destinationUri)
            verify(fileSystemRepository).copyFiles(file, destination)
        }
}