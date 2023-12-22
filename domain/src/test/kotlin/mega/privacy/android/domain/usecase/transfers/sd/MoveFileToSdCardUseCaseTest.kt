package mega.privacy.android.domain.usecase.transfers.sd

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MoveFileToSdCardUseCaseTest {
    private lateinit var underTest: MoveFileToSdCardUseCase

    private val settingsRepository = mock<SettingsRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MoveFileToSdCardUseCase(
            settingsRepository,
            fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository, fileSystemRepository)
    }

    @Test
    fun `test that the file is not moved if sd card uri is not set`() = runTest {
        whenever(settingsRepository.getDownloadToSdCardUri()).thenReturn(null)
        underTest(mock(), "destination")
        verifyNoInteractions(fileSystemRepository)
    }

    @Test
    fun `test that file is not moved to destination using sd card uri`() = runTest {
        val file = mock<File>()
        val destination = "destination"
        val uri = "sdcard:uri"
        whenever(settingsRepository.getDownloadToSdCardUri()).thenReturn(uri)
        whenever(fileSystemRepository.moveFileToSd(any(), any(), any())).thenReturn(true)
        underTest(file, destination)
        verify(fileSystemRepository).moveFileToSd(file, destination, uri)
    }
}