package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.ResourceAlreadyExistsMegaException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOrCreateMyChatsFilesFolderIdUseCaseTest {
    private lateinit var underTest: GetOrCreateMyChatsFilesFolderIdUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val chatRepository = mock<ChatRepository>()
    private val createFolderNodeUseCase = mock<CreateFolderNodeUseCase>()
    private val isNodeInRubbishOrDeletedUseCase = mock<IsNodeInRubbishOrDeletedUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val getChildNodeUseCase = mock<GetChildNodeUseCase>()

    @BeforeAll
    fun setup() {
        underTest = GetOrCreateMyChatsFilesFolderIdUseCase(
            createFolderNodeUseCase = createFolderNodeUseCase,
            fileSystemRepository = fileSystemRepository,
            chatRepository = chatRepository,
            isNodeInRubbishOrDeletedUseCase = isNodeInRubbishOrDeletedUseCase,
            getChildNodeUseCase = getChildNodeUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            fileSystemRepository,
            chatRepository,
            createFolderNodeUseCase,
            isNodeInRubbishOrDeletedUseCase,
            getRootNodeUseCase,
            getChildNodeUseCase,
        )

    @Test
    fun `test that repository folder id is returned when the folder exists and is not in the rubbish bin`() =
        runTest {
            val folderId = NodeId(11L)
            whenever(fileSystemRepository.getMyChatsFilesFolderId()).thenReturn(folderId)
            whenever(isNodeInRubbishOrDeletedUseCase(folderId.longValue)).thenReturn(false)
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
    fun `test that created folder is returned when existing folder id is in rubbish bin`() =
        runTest {
            val handle = 11L
            val folderId = NodeId(handle)
            whenever(fileSystemRepository.getMyChatsFilesFolderId()).thenReturn(folderId)
            whenever(isNodeInRubbishOrDeletedUseCase(folderId.longValue)).thenReturn(true)
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
        verify(createFolderNodeUseCase).invoke(folderName)
    }

    @Test
    fun `test that exception is thrown when create folder fails`() = runTest {
        stubFolderCreation()
        whenever(createFolderNodeUseCase(any(), anyOrNull())).thenThrow(IllegalArgumentException())
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

    @Test
    fun `test that pre-existing folder is set to MyChatFilesFolder when a normal folder with default chat folder name already exists`() =
        runTest {
            val folderName = "My chat files"
            val folderHandle = 11L
            val rootNodeHandle = 0L
            val rootNode = mock<Node> {
                on { id } doReturn NodeId(rootNodeHandle)
            }

            whenever(fileSystemRepository.getMyChatsFilesFolderId()).thenReturn(null)
            whenever(chatRepository.getDefaultChatFolderName()).thenReturn(folderName)
            whenever(fileSystemRepository.setMyChatFilesFolder(any())).thenReturn(folderHandle)
            whenever(createFolderNodeUseCase(folderName)).thenAnswer {
                throw ResourceAlreadyExistsMegaException(
                    errorCode = 0,
                    errorString = null,
                    value = folderHandle,
                )
            }

            whenever(getRootNodeUseCase()).thenReturn(rootNode)

            val existingFolderNode = mock<FolderNode> {
                on { id } doReturn NodeId(folderHandle)
            }
            whenever(getChildNodeUseCase(NodeId(rootNodeHandle), folderName))
                .thenReturn(existingFolderNode)

            assertThat(underTest()).isEqualTo(NodeId(folderHandle))
        }

    @Test
    fun `test that exception is thrown when create chat files folder fails due to error that is other than already_exists`() =
        runTest {
            val folderName = "My chat files"
            val folderHandle = 11L

            whenever(fileSystemRepository.getMyChatsFilesFolderId()).thenReturn(null)
            whenever(chatRepository.getDefaultChatFolderName()).thenReturn(folderName)
            whenever(fileSystemRepository.setMyChatFilesFolder(any())).thenReturn(folderHandle)
            whenever(createFolderNodeUseCase(folderName)).thenAnswer {
                throw NotEnoughQuotaMegaException(
                    errorCode = 0,
                    errorString = null,
                )
            }

            assertThrows<MegaException> { underTest() }
        }

    private suspend fun stubFolderCreation(handle: Long = 11L, folderName: String = "Folder name") {
        whenever(fileSystemRepository.getMyChatsFilesFolderId()).thenReturn(null)
        whenever(chatRepository.getDefaultChatFolderName()).thenReturn(folderName)
        whenever(fileSystemRepository.setMyChatFilesFolder(any())).thenReturn(handle)
        whenever(createFolderNodeUseCase(any(), anyOrNull())).thenReturn(NodeId(handle))
    }
}
