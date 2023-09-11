package mega.privacy.android.domain.usecase.backup

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
import org.mockito.kotlin.verify

/**
 * Test class for [SaveBackupUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveBackupUseCaseTest {

    private lateinit var underTest: SaveBackupUseCase

    private val backupRepository = mock<BackupRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SaveBackupUseCase(backupRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(backupRepository)
    }

    @Test
    fun `test that backup is saved when invoked`() = runTest {
        val backup = mock<Backup>()
        underTest.invoke(backup)
        verify(backupRepository).saveBackup(backup)
    }
}
