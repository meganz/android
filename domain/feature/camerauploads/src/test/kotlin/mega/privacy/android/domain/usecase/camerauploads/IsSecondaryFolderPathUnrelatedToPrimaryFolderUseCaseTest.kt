package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [IsSecondaryFolderPathUnrelatedToPrimaryFolderUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsSecondaryFolderPathUnrelatedToPrimaryFolderUseCaseTest {

    private lateinit var underTest: IsSecondaryFolderPathUnrelatedToPrimaryFolderUseCase

    private val getPrimaryFolderPathUseCase = mock<GetPrimaryFolderPathUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = IsSecondaryFolderPathUnrelatedToPrimaryFolderUseCase(
            getPrimaryFolderPathUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getPrimaryFolderPathUseCase)
    }

    @Test
    fun `test that the secondary folder is unrelated if the primary folder path is empty`() =
        runTest {
            whenever(getPrimaryFolderPathUseCase()).thenReturn("")

            assertThat(underTest("secondary/folder/path")).isTrue()
        }

    @Test
    fun `test that the secondary folder is unrelated if the primary folder path only contains whitespaces`() =
        runTest {
            whenever(getPrimaryFolderPathUseCase()).thenReturn(" ")

            assertThat(underTest("secondary/folder/path")).isTrue()
        }

    @ParameterizedTest(name = "and the secondary folder path is \"{0}\", while the primary folder path is \"{1}\"")
    @MethodSource("provideUnrelatedFoldersParameters")
    fun `test that the secondary folder is unrelated if the primary folder path exists`(
        secondaryFolderPath: String,
        primaryFolderPath: String,
    ) = runTest {
        whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)

        assertThat(underTest(secondaryFolderPath)).isTrue()
    }

    private fun provideUnrelatedFoldersParameters() = Stream.of(
        Arguments.of("A/B/C", "A/B/CD"),
        Arguments.of("A/B/CD", "A/B/C"),
        Arguments.of("A", "B"),
        Arguments.of("B", "A"),
    )

    @ParameterizedTest(name = "and the secondary folder path is \"{0}\", while the primary folder path is \"{1}\"")
    @MethodSource("provideRelatedFoldersParameters")
    fun `test that the secondary folder is related if the primary folder path exists`(
        secondaryFolderPath: String,
        primaryFolderPath: String,
    ) = runTest {
        whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)

        assertThat(underTest(secondaryFolderPath)).isFalse()
    }

    private fun provideRelatedFoldersParameters() = Stream.of(
        Arguments.of("A/B/C", "A/B/C"),
        Arguments.of("A/B", "A/B/C"),
        Arguments.of("A/B/C", "A/B"),
        Arguments.of("A", "A"),
    )
}