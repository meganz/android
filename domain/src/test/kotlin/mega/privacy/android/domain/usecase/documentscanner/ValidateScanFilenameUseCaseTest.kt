package mega.privacy.android.domain.usecase.documentscanner

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.documentscanner.ScanFilenameValidationStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Test class for [ValidateScanFilenameUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ValidateScanFilenameUseCaseTest {

    private lateinit var underTest: ValidateScanFilenameUseCase

    @BeforeEach
    fun reset() {
        underTest = ValidateScanFilenameUseCase()
    }

    @ParameterizedTest(name = "filename: \"{0}\"")
    @ValueSource(strings = ["", "  "])
    fun `test that an empty filename validation status is returned if the filename does not contain anything`(
        filename: String,
    ) = runTest {
        assertThat(underTest(filename)).isEqualTo(ScanFilenameValidationStatus.EmptyFilename)
    }

    @ParameterizedTest(name = "invalid character: {0}")
    @ValueSource(strings = ["/", "\\", ":", "?", "\"", "*", "<", ">", "|"])
    fun `test that an invalid filename validation status is returned if the non-empty filename contains any of the invalid characters`(
        character: String,
    ) = runTest {
        assertThat(underTest("Test$character")).isEqualTo(ScanFilenameValidationStatus.InvalidFilename)
    }

    @Test
    fun `test that a valid filename validation status is returned if all checks pass`() =
        runTest {
            assertThat(underTest("Test Filename")).isEqualTo(ScanFilenameValidationStatus.ValidFilename)
        }
}