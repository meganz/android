package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [IsSecondaryFolderPathValidUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsSecondaryFolderPathValidUseCaseTest {

    private lateinit var underTest: IsSecondaryFolderPathValidUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsSecondaryFolderPathValidUseCase(
            cameraUploadRepository = cameraUploadRepository,
            fileSystemRepository = fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            fileSystemRepository,
        )
    }

    @ParameterizedTest(name = "path: {0}")
    @MethodSource("provideInvalidParameters")
    fun `test that the secondary folder path is invalid if it is null or empty`(path: String?) =
        runTest {
            assertThat(underTest(path)).isFalse()
        }

    @ParameterizedTest(name = "does folder exists: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the secondary folder is valid or not if it exists`(doesFolderExists: Boolean) =
        runTest {
            val testPrimaryFolderPath = "test/primary/folder/path"
            val testSecondaryFolderPath = "test/secondary/folder/path"

            whenever(cameraUploadRepository.getPrimaryFolderLocalPath()).thenReturn(
                testPrimaryFolderPath
            )
            whenever(fileSystemRepository.doesFolderExists(any())).thenReturn(doesFolderExists)

            val expectedAnswer = underTest(testSecondaryFolderPath)
            if (doesFolderExists) {
                assertThat(expectedAnswer).isTrue()
            } else {
                assertThat(expectedAnswer).isFalse()
            }
        }

    @ParameterizedTest(name = "when primary path is {0} and secondary is {1}, then is secondary valid is {2}")
    @MethodSource("providePathParameters")
    fun `test that the primary folder is valid or not when compared to the secondary folder path`(
        primaryFolderPath: String,
        secondaryFolderPath: String,
        isSecondaryFolderValid: Boolean,
    ) = runTest {
        whenever(cameraUploadRepository.getPrimaryFolderLocalPath()).thenReturn(
            primaryFolderPath
        )
        whenever(fileSystemRepository.doesFolderExists(any())).thenReturn(true)

        val expectedAnswer = underTest(secondaryFolderPath)
        assertThat(expectedAnswer).isEqualTo(isSecondaryFolderValid)
    }

    private fun provideInvalidParameters() = Stream.of(
        Arguments.of(null),
        Arguments.of(""),
        Arguments.of(" "),
    )

    private fun providePathParameters() = Stream.of(
        Arguments.of("test/path", "test/path", false),
        Arguments.of("test/path", "test/path/2", true),
    )
}
