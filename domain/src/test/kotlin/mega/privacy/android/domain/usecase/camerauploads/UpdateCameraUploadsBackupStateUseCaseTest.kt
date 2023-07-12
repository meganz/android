package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateCameraUploadsBackupStateUseCaseTest {
    lateinit var underTest: UpdateCameraUploadsBackupStateUseCase

    private val updateCameraUploadsStateUseCase = mock<UpdateCameraUploadsBackupUseCase>()
    private val updateMediaUploadsBackupUseCase = mock<UpdateMediaUploadsBackupUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateCameraUploadsBackupStateUseCase(
            updateCameraUploadsStateUseCase = updateCameraUploadsStateUseCase,
            updateMediaUploadsBackupUseCase = updateMediaUploadsBackupUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            updateCameraUploadsStateUseCase,
            updateMediaUploadsBackupUseCase,
        )
    }

    @Test
    fun `test that primary and secondary backup state is updated`() = runTest {
        val backupState = BackupState.ACTIVE
        val primaryFolderName = "Camera Uploads"
        val secondaryFolderName = "Media Uploads"

        underTest.invoke(backupState, primaryFolderName, secondaryFolderName)
        verify(updateCameraUploadsStateUseCase).invoke(primaryFolderName, backupState)
        verify(updateMediaUploadsBackupUseCase).invoke(secondaryFolderName, backupState)
    }

}
