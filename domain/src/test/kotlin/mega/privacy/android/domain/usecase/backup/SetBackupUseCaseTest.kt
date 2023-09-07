package mega.privacy.android.domain.usecase.backup

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.Backup
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
        val backupType = 1
        val targetNode = 1L
        val localFolder = "folderName"
        val backupName = "backupName"
        val state = 2
        val subState = 3
        val backup = mock<Backup>()

        whenever(
            backupRepository.setBackup(
                backupType = backupType,
                targetNode = targetNode,
                localFolder = localFolder,
                backupName = backupName,
                state = state,
                subState = subState,
            )
        ).thenReturn(backup)

        val actual = underTest(
            backupType = backupType,
            targetNode = targetNode,
            localFolder = localFolder,
            backupName = backupName,
            state = state,
            subState = subState,
        )
        Truth.assertThat(actual).isEqualTo(backup)
    }
}
