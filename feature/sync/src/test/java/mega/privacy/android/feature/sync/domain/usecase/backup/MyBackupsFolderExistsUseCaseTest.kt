package mega.privacy.android.feature.sync.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BackupRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyBackupsFolderExistsUseCaseTest {
    private lateinit var underTest: MyBackupsFolderExistsUseCase
    private val backupRepository = mock<BackupRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MyBackupsFolderExistsUseCase(backupRepository = backupRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(backupRepository)
    }

    @Test
    fun `test that result is false if the folder does not exist`() = runTest {
        whenever(backupRepository.myBackupsFolderExists()).thenReturn(false)
        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that result is true if the folder exists`() = runTest {
        whenever(backupRepository.myBackupsFolderExists()).thenReturn(true)
        assertThat(underTest()).isTrue()
    }
}