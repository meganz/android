package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [IsSecondaryFolderPathValidUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsSecondaryFolderPathValidUseCaseTest {

    private lateinit var underTest: IsSecondaryFolderPathValidUseCase

    private val getPrimaryFolderPathUseCase = mock<GetPrimaryFolderPathUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsSecondaryFolderPathValidUseCase(
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
            fileSystemRepository = fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getPrimaryFolderPathUseCase, fileSystemRepository)
    }

    @ParameterizedTest(name = "path: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = [" "])
    fun `test that the secondary folder path is invalid if it is null or empty`(path: String?) =
        runTest {
            assertThat(underTest(path)).isFalse()
        }

    @Test
    fun `test that the secondary folder path is invalid if it does not exist in the file system`() =
        runTest {
            whenever(fileSystemRepository.doesFolderExists(any())).thenReturn(false)

            assertThat(underTest("new/path")).isFalse()
        }

    @Test
    fun `test that the secondary folder path is valid if it exists in the file system and the primary folder path is empty`() =
        runTest {
            whenever(fileSystemRepository.doesFolderExists(any())).thenReturn(true)
            whenever(getPrimaryFolderPathUseCase()).thenReturn("")

            assertThat(underTest("new/path")).isTrue()
        }

    @ParameterizedTest(name = "when secondary folder path is \"{0}\" and primary folder path is \"{1}\", then is secondary folder valid is {2}")
    @MethodSource("provideFolderParameters")
    fun `test that the secondary folder path is valid or not if it exists in the file system and the primary folder path exists`(
        secondaryFolderPath: String,
        primaryFolderPath: String,
        isSecondaryFolderValid: Boolean,
    ) = runTest {
        whenever(fileSystemRepository.doesFolderExists(any())).thenReturn(true)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)

        assertThat(underTest(secondaryFolderPath)).isEqualTo(isSecondaryFolderValid)
    }

    private fun provideFolderParameters() = Stream.of(
        Arguments.of("A/B/C", "A/B/C", false),
        Arguments.of("A/B/C", "A/B/CD", true),
        Arguments.of("A/B/CD", "A/B/C", true),
        Arguments.of("A/B", "A/B/C", false),
        Arguments.of("A/B/C", "A/B", false),
        Arguments.of("A", "A", false),
        Arguments.of("A", "B", true),
        Arguments.of("B", "A", true),
    )
}
