package mega.privacy.android.domain.usecase.offline

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsOfflinePathUseCaseTest {

    private val fileSystemRepository: FileSystemRepository = mock()

    private lateinit var underTest: IsOfflinePathUseCase

    @BeforeAll
    fun setup() {
        underTest = IsOfflinePathUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that invoke returns true when the path begins with the offline path`() =
        runTest {
            stubPaths()
            Truth.assertThat(underTest(pathInOffline)).isTrue()
        }

    @Test
    fun `test that invoke returns true when the path begins with the backup offline path`() =
        runTest {
            stubPaths()
            Truth.assertThat(underTest(pathInBackupOffline)).isTrue()
        }

    @Test
    fun `test that invoke returns false when the path does not begin with the offline or backup offline path`() =
        runTest {
            stubPaths()
            Truth.assertThat(underTest(randomPath)).isFalse()
        }

    private fun stubPaths() = runTest {
        whenever(fileSystemRepository.getOfflinePath()).thenReturn(offlinePath)
        whenever(fileSystemRepository.getOfflineBackupsPath()).thenReturn(offlineBackupPath)
    }

    companion object {
        private const val fileName = "file.txt"
        private const val offlinePath = "offlinePath"
        private const val offlineBackupPath = "offlinePath/in"
        private const val pathInOffline = "$offlinePath/$fileName"
        private const val pathInBackupOffline = "$offlineBackupPath/$fileName"
        private const val randomPath = "downloads/$fileName"
    }
}