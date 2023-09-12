package mega.privacy.android.domain.usecase.backup

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.BackupRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [SetBackupUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetBackupUseCaseTest {

    private lateinit var underTest: SetBackupUseCase

    private val backupRepository = mock<BackupRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetBackupUseCase(backupRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(backupRepository)
    }

    @Test
    fun `test that when invoked it returns a backup`() = runTest {
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val targetNode = 1L
        val localFolder = "folderName"
        val backupName = "backupName"
        val state = BackupState.ACTIVE
        val backup = mock<Backup>()

        whenever(
            backupRepository.setBackup(
                backupType = backupType,
                targetNode = targetNode,
                localFolder = localFolder,
                backupName = backupName,
                state = state,
            )
        ).thenReturn(backup)

        val actual = underTest(
            backupType = backupType,
            targetNode = targetNode,
            localFolder = localFolder,
            backupName = backupName,
            state = state,
        )
        Truth.assertThat(actual).isEqualTo(backup)
    }
}
