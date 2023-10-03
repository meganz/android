package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateTempFileAndRemoveCoordinatesUseCaseTest {
    lateinit var underTest: CreateTempFileAndRemoveCoordinatesUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = CreateTempFileAndRemoveCoordinatesUseCase(
            fileSystemRepository = fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            fileSystemRepository,
        )
    }

    @Test
    fun `test that fileSystemRepository createTempFile is invoked with correct parameter`() =
        runTest {
            val rootPath = "rootPath"
            val filePath = "filePath"
            val destinationPath = "destinationPath"
            val timestamp = 12345678L

            val tempFilePath = "tempFilePath"

            whenever(fileSystemRepository.createTempFile(rootPath, filePath, destinationPath))
                .thenReturn(tempFilePath)

            underTest.invoke(rootPath, filePath, destinationPath, timestamp)
            verify(fileSystemRepository).createTempFile(rootPath, filePath, destinationPath)
        }

    @Test
    fun `test that fileSystemRepository removeGPSCoordinates is invoked with the result of createTempFile`() =
        runTest {
            val rootPath = "rootPath"
            val filePath = "filePath"
            val destinationPath = "destinationPath"
            val timestamp = 12345678L

            val tempFilePath = "tempFilePath"

            whenever(fileSystemRepository.createTempFile(rootPath, filePath, destinationPath))
                .thenReturn(tempFilePath)

            underTest.invoke(rootPath, filePath, destinationPath, timestamp)
            verify(fileSystemRepository).removeGPSCoordinates(tempFilePath)
        }

    @Test
    fun `test that set last modified to the new file is invoked with timestamp passed in parameter`() =
        runTest {
            val rootPath = "rootPath"
            val filePath = "filePath"
            val destinationPath = "destinationPath"
            val timestamp = 12345678L

            val tempFilePath = "tempFilePath"
            whenever(fileSystemRepository.createTempFile(rootPath, filePath, destinationPath))
                .thenReturn(tempFilePath)

            underTest.invoke(rootPath, filePath, destinationPath, timestamp)
            verify(fileSystemRepository).setLastModified(tempFilePath, timestamp)
        }
}
