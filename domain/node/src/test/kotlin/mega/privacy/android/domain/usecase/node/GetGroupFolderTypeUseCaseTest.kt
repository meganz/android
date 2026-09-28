package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetGroupFolderTypeUseCaseTest {

    private val cameraUploadsRepository: CameraUploadsRepository = mock()
    private val chatRepository: ChatRepository = mock()
    private val monitorBackupFolder: MonitorBackupFolder = mock()
    private lateinit var getGroupFolderTypeUseCase: GetGroupFolderTypeUseCase

    @BeforeAll
    fun setUp() {
        getGroupFolderTypeUseCase =
            GetGroupFolderTypeUseCase(cameraUploadsRepository, chatRepository, monitorBackupFolder)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository, chatRepository, monitorBackupFolder)
    }

    @Test
    fun `test that use case returns correctly`() = runTest {
        val expectedMap = mapOf(
            NodeId(1) to FolderType.MediaSyncFolder,
            NodeId(2) to FolderType.MediaSyncFolder,
            NodeId(3) to FolderType.ChatFilesFolder,
            NodeId(4) to FolderType.RootBackup
        )

        whenever(cameraUploadsRepository.getPrimarySyncHandle()).thenReturn(1L)
        whenever(cameraUploadsRepository.getSecondarySyncHandle()).thenReturn(2L)
        whenever(chatRepository.getChatFilesFolderId()).thenReturn(NodeId(3))
        whenever(monitorBackupFolder()).thenReturn(flowOf(Result.success(NodeId(4))))

        assertThat(getGroupFolderTypeUseCase.invoke()).isEqualTo(expectedMap)
    }
}