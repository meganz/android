package mega.privacy.android.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.repository.BackupRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetBackupInfoUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetBackupInfoUseCaseTest {

    private lateinit var underTest: GetBackupInfoUseCase

    private val backupRepository = mock<BackupRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetBackupInfoUseCase(backupRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(backupRepository)
    }

    @Test
    fun `test that get backup info is invoked`() = runTest {
        val backupInfoList = listOf<BackupInfo>(
            mock { on { id }.thenReturn(12345L) },
            mock { on { id }.thenReturn(67890L) },
        )
        whenever(backupRepository.getBackupInfo()).thenReturn(backupInfoList)
        assertThat(underTest()).isEqualTo(backupInfoList)
    }
}