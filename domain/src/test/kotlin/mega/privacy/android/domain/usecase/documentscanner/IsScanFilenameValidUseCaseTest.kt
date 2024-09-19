package mega.privacy.android.domain.usecase.documentscanner

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Test class for [IsScanFilenameValidUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsScanFilenameValidUseCaseTest {

    private lateinit var underTest: IsScanFilenameValidUseCase

    @BeforeEach
    fun reset() {
        underTest = IsScanFilenameValidUseCase()
    }

    @Test
    fun `test that the filename is invalid if it is empty`() {
        assertThat(underTest("")).isFalse()
    }

    @Test
    fun `test that the filename is invalid if it only contains whitespaces`() {
        assertThat(underTest(" ")).isFalse()
    }

    @ParameterizedTest(name = "invalid character: {0}")
    @ValueSource(strings = ["/", ":", "?", "\"", "*", "<", ">", "|"])
    fun `test that the filename is invalid when it contains any of the invalid characters`(character: String) =
        runTest {
            val filename = "Filename$character"
            assertThat(underTest(filename)).isFalse()
        }
}