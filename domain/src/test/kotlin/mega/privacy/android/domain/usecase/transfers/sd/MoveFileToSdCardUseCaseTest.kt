package mega.privacy.android.domain.usecase.transfers.sd

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MoveFileToSdCardUseCaseTest {
    private lateinit var underTest: MoveFileToSdCardUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MoveFileToSdCardUseCase(
            fileSystemRepository,
            settingsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            fileSystemRepository,
            settingsRepository,
        )
    }

    @Test
    fun `test that file is moved to destination `() = runTest {
        val file = mock<File>()
        val destination = "content:destination/root/"
        val subFolders = listOf("subfolder1", "subfolder2")
        whenever(fileSystemRepository.moveFileToSd(any(), any(), any())).thenReturn(true)
        underTest(file, destination, subFolders)
        verify(fileSystemRepository).moveFileToSd(file, destination, subFolders)
    }

    @Test
    fun `test that when destination uri is not valid then it throws an excepcion`() =
        runTest {
            val file = mock<File>()
            val destination = "/storage/root/cache/"
            val subFolders = listOf("subfolder1", "subfolder2")
            whenever(settingsRepository.getDownloadToSdCardUri()).thenReturn(null)
            assertThrows<IllegalArgumentException> {
                underTest(file, destination, subFolders)
                verify(fileSystemRepository).moveFileToSd(file, destination, subFolders)
            }
        }
}