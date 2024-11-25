package mega.privacy.android.domain.usecase.link

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DecodeLinkUseCaseTest {
    private val underTest = DecodeLinkUseCase()

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("urls")
    fun `test that query parameter is stripped when url is `(
        url: String,
        expected: String
    ) {
        val result = underTest(url)
        assertEquals(expected, result)
    }

    private fun urls(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "https://mega.nz/?sort=10&keyword=mega&tracking_id=6423764278462",
            "https://mega.nz/"
        ),
        Arguments.of(
            "https://mega.nz/file/O3JAwICB#abc?sort=10",
            "https://mega.nz/file/O3JAwICB#abc"
        ),
        Arguments.of(
            "https://mega.nz/folder/PiZGmBgJ#_JutIj2nSgYF49pFo9pBNA?qwerqwer",
            "https://mega.nz/folder/PiZGmBgJ#_JutIj2nSgYF49pFo9pBNA"
        ),
        Arguments.of(
            "https://mega.nz/folder/PiZGmBgJ#_JutIj2nSgYF49pFo9pBNA",
            "https://mega.nz/folder/PiZGmBgJ#_JutIj2nSgYF49pFo9pBNA"
        ),
    )
}