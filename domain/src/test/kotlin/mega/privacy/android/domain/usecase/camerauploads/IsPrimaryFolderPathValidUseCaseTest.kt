package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
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
 * Test class for [IsPrimaryFolderPathValidUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsPrimaryFolderPathValidUseCaseTest {

    private lateinit var underTest: IsPrimaryFolderPathValidUseCase

    private val getSecondaryFolderPathUseCase = mock<GetSecondaryFolderPathUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()

    @BeforeAll
    fun setUp() {
        underTest = IsPrimaryFolderPathValidUseCase(
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase,
            fileSystemRepository = fileSystemRepository,
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getSecondaryFolderPathUseCase,
            fileSystemRepository,
            isSecondaryFolderEnabled,
        )
    }

    @ParameterizedTest(name = "path: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = [" "])
    fun `test that the primary folder path is invalid if it is null or empty`(path: String?) =
        runTest {
            assertThat(underTest(path)).isFalse()
        }

    @Test
    fun `test that the primary folder path is invalid if it does not exist in the file system`() =
        runTest {
            whenever(fileSystemRepository.doesFolderExists(any())).thenReturn(false)

            assertThat(underTest("new/path")).isFalse()
        }

    @Test
    fun `test that the primary folder path is valid if it exists is the file system and secondary folder uploads are disabled`() =
        runTest {
            whenever(fileSystemRepository.doesFolderExists(any())).thenReturn(true)
            whenever(isSecondaryFolderEnabled()).thenReturn(false)

            assertThat(underTest("new/path")).isTrue()
        }

    @Test
    fun `test that the primary folder path is valid if it exists in the file system and secondary folder uploads are enabled but the secondary folder path is empty`() =
        runTest {
            whenever(fileSystemRepository.doesFolderExists(any())).thenReturn(true)
            whenever(isSecondaryFolderEnabled()).thenReturn(false)
            whenever(getSecondaryFolderPathUseCase()).thenReturn("")

            assertThat(underTest("new/path")).isTrue()
        }

    @ParameterizedTest(name = "when primary folder path is \"{0}\" and secondary folder path is \"{1}\", then is primary folder valid is {2}")
    @MethodSource("provideFolderParameters")
    fun `test that the primary folder path is valid or not if it exists in the file system and secondary folder uploads are enabled and the secondary folder path exists`(
        primaryFolderPath: String,
        secondaryFolderPath: String,
        isPrimaryFolderValid: Boolean,
    ) = runTest {
        whenever(fileSystemRepository.doesFolderExists(any())).thenReturn(true)
        whenever(isSecondaryFolderEnabled()).thenReturn(true)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(secondaryFolderPath)

        assertThat(underTest(primaryFolderPath)).isEqualTo(isPrimaryFolderValid)
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
