package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMyChatsFilesFolderIdUseCaseTest {
    private lateinit var underTest: GetMyChatsFilesFolderIdUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val nodeRepository = mock<NodeRepository>()
    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetMyChatsFilesFolderIdUseCase(
            fileSystemRepository,
            nodeRepository,
            chatRepository,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            fileSystemRepository,
            nodeRepository,
            chatRepository,
        )

    @Test
    fun `test that repository folder id is returned when the folder exists`() = runTest {
        val folderId = NodeId(11L)
        whenever(fileSystemRepository.getMyChatsFilesFolderId()).thenReturn(folderId)
        whenever(nodeRepository.getNodeById(folderId)).thenReturn(mock())
        val actual = underTest()
        assertThat(actual).isEqualTo(folderId)
    }

    @Test
    fun `test that created folder is returned when existing folder id is null`() = runTest {
        val handle = 11L
        val folderId = NodeId(handle)
        stubFolderCreation()
        val actual = underTest()
        assertThat(actual).isEqualTo(folderId)
    }

    @Test
    fun `test that folder is created with the proper name`() = runTest {
        val folderName = "folder name"
        stubFolderCreation(folderName = folderName)
        underTest()
        verify(chatRepository).getDefaultChatFolderName()
        verify(fileSystemRepository).createFolder(folderName)
    }

    @Test
    fun `test that exception is thrown when create folder fails`() = runTest {
        stubFolderCreation()
        whenever(fileSystemRepository.createFolder(any())).thenReturn(null)
        assertThrows<Exception> {
            underTest()
        }
    }

    @Test
    fun `test that exception is thrown when set folder fails`() = runTest {
        stubFolderCreation()
        whenever(fileSystemRepository.setMyChatFilesFolder(any())).thenReturn(null)
        assertThrows<Exception> {
            underTest()
        }
    }

    private suspend fun stubFolderCreation(handle: Long = 11L, folderName: String = "Folder name") {
        whenever(fileSystemRepository.getMyChatsFilesFolderId()).thenReturn(null)
        whenever(chatRepository.getDefaultChatFolderName()).thenReturn(folderName)
        whenever(fileSystemRepository.setMyChatFilesFolder(any())).thenReturn(handle)
        whenever(fileSystemRepository.createFolder(any())).thenReturn(handle)
    }
}