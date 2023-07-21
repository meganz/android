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

/**
 * Test class for [UpdateCameraUploadsBackupStatesUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateCameraUploadsBackupStatesUseCaseTest {
    lateinit var underTest: UpdateCameraUploadsBackupStatesUseCase

    private val updatePrimaryFolderBackupStateUseCase =
        mock<UpdatePrimaryFolderBackupStateUseCase>()
    private val updateSecondaryFolderBackupStateUseCase =
        mock<UpdateSecondaryFolderBackupStateUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateCameraUploadsBackupStatesUseCase(
            updatePrimaryFolderBackupStateUseCase = updatePrimaryFolderBackupStateUseCase,
            updateSecondaryFolderBackupStateUseCase = updateSecondaryFolderBackupStateUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            updatePrimaryFolderBackupStateUseCase,
            updateSecondaryFolderBackupStateUseCase,
        )
    }

    @Test
    fun `test that the primary and secondary backup states are updated`() = runTest {
        val backupState = BackupState.ACTIVE

        underTest.invoke(backupState)
        verify(updatePrimaryFolderBackupStateUseCase).invoke(backupState)
        verify(updateSecondaryFolderBackupStateUseCase).invoke(backupState)
    }
}
