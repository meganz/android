package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.exception.DotNameException
import mega.privacy.android.domain.exception.DoubleDotNameException
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.repository.RegexRepository
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.regex.Pattern

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidateNodeNameCharactersUseCaseTest {

    private val regexRepository: RegexRepository = mock {
        on { invalidNamePattern } doReturn Pattern.compile(INVALID_NAME_REGEX)
    }

    private val underTest = ValidateNodeNameCharactersUseCase(regexRepository)

    @Test
    fun `test that invoke throws EmptyNodeNameException when name is empty`() {
        assertThrows<EmptyNodeNameException> { underTest("") }
    }

    @Test
    fun `test that invoke throws DotNameException when name is a single dot`() {
        assertThrows<DotNameException> { underTest(".") }
    }

    @Test
    fun `test that invoke throws DoubleDotNameException when name is a double dot`() {
        assertThrows<DoubleDotNameException> { underTest("..") }
    }

    @ParameterizedTest(name = "name: {0}")
    @ValueSource(
        strings = [
            "a/b",
            "../etc/passwd",
            "a*b",
            "a|b",
            "a?b",
            "a:b",
            "a\"b",
            "a<b",
            "a>b",
        ]
    )
    fun `test that invoke throws InvalidNodeNameException when name contains a reserved character`(
        name: String,
    ) {
        assertThrows<InvalidNodeNameException> { underTest(name) }
    }

    @Test
    fun `test that invoke throws InvalidNodeNameException when name contains a backslash`() {
        assertThrows<InvalidNodeNameException> { underTest("..\\Windows") }
    }

    @Test
    fun `test that invoke throws InvalidNodeNameException for every C0 and DEL control character`() {
        val controlCodePoints = (0x00..0x1F) + 0x7F
        controlCodePoints.forEach { codePoint ->
            assertThrows<InvalidNodeNameException>("code point $codePoint was accepted") {
                underTest("a${codePoint.toChar()}b")
            }
        }
    }

    @ParameterizedTest(name = "name: {0}")
    @ValueSource(
        strings = [
            "document.pdf",
            "My Folder",
            "...",
            ".hidden",
            "a.b.c",
            "price \$100",
            "ten tieng Viet",
            "emoji-file",
        ]
    )
    fun `test that invoke does not throw when name is valid`(name: String) {
        assertDoesNotThrow { underTest(name) }
    }

    private companion object {
        // Mirrors RegexRepositoryImpl.INVALID_NAME_REGEX.
        const val INVALID_NAME_REGEX = "[*|\\?:\"<>\\\\\\\\/\\x00-\\x1F\\x7F]"
    }
}
