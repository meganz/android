package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.usecase.camerauploads.RemoveBackupFolderUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class RemoveBackupFoldersLogoutTaskTest {
    private lateinit var underTest: RemoveBackupFoldersLogoutTask

    private val removeBackupFolderUseCase = mock<RemoveBackupFolderUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = RemoveBackupFoldersLogoutTask(
            removeBackupFolderUseCase = removeBackupFolderUseCase
        )
    }

    @Test
    internal fun `test that remove backup folder use case is called with correct parameters`() =
        runTest {
            underTest()

            verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Primary)
            verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Secondary)
        }
}