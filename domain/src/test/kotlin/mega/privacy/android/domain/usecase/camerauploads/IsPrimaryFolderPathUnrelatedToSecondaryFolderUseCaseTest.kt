package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
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
 * Test class for [IsPrimaryFolderPathUnrelatedToSecondaryFolderUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsPrimaryFolderPathUnrelatedToSecondaryFolderUseCaseTest {

    private lateinit var underTest: IsPrimaryFolderPathUnrelatedToSecondaryFolderUseCase

    private val getSecondaryFolderPathUseCase = mock<GetSecondaryFolderPathUseCase>()
    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()

    @BeforeAll
    fun setUp() {
        underTest = IsPrimaryFolderPathUnrelatedToSecondaryFolderUseCase(
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase,
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getSecondaryFolderPathUseCase, isSecondaryFolderEnabled)
    }

    @Test
    fun `test that the primary folder is unrelated if secondary uploads are disabled`() = runTest {
        whenever(isSecondaryFolderEnabled()).thenReturn(false)

        assertThat(underTest("primary/folder/path")).isTrue()
    }

    @Test
    fun `test that the primary folder is unrelated if the secondary folder path is empty`() =
        runTest {
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(getSecondaryFolderPathUseCase()).thenReturn("")

            assertThat(underTest("primary/folder/path")).isTrue()
        }

    @Test
    fun `test that the primary folder is unrelated if the secondary folder path only contains whitespaces`() =
        runTest {
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(getSecondaryFolderPathUseCase()).thenReturn(" ")

            assertThat(underTest("primary/folder/path")).isTrue()
        }

    @ParameterizedTest(name = "and the primary folder path is \"{0}\", while the secondary folder path is \"{1}\"")
    @MethodSource("provideUnrelatedFoldersParameters")
    fun `test that the primary folder is unrelated if the secondary folder path exists`(
        primaryFolderPath: String,
        secondaryFolderPath: String,
    ) = runTest {
        whenever(isSecondaryFolderEnabled()).thenReturn(true)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(secondaryFolderPath)

        assertThat(underTest(primaryFolderPath)).isTrue()
    }

    private fun provideUnrelatedFoldersParameters() = Stream.of(
        Arguments.of("A/B/C", "A/B/CD"),
        Arguments.of("A/B/CD", "A/B/C"),
        Arguments.of("A", "B"),
        Arguments.of("B", "A"),
    )

    @ParameterizedTest(name = "and the primary folder path is \"{0}\", while the secondary folder path is \"{1}\"")
    @MethodSource("provideRelatedFoldersParameters")
    fun `test that the primary folder is related if the secondary folder path exists`(
        primaryFolderPath: String,
        secondaryFolderPath: String,
    ) = runTest {
        whenever(isSecondaryFolderEnabled()).thenReturn(true)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(secondaryFolderPath)

        assertThat(underTest(primaryFolderPath)).isFalse()
    }

    private fun provideRelatedFoldersParameters() = Stream.of(
        Arguments.of("A/B/C", "A/B/C"),
        Arguments.of("A/B", "A/B/C"),
        Arguments.of("A/B/C", "A/B"),
        Arguments.of("A", "A"),
    )
}