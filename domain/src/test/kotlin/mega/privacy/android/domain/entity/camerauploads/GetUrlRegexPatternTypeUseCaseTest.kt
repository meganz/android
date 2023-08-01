package mega.privacy.android.domain.entity.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.GetUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.IsUrlMatchesRegexUseCase
import mega.privacy.android.domain.usecase.IsUrlWhitelistedUseCase
import mega.privacy.android.domain.usecase.IsUrlWhitelistedUseCase.Companion.PLAY_STORE_URL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUrlRegexPatternTypeUseCaseTest {
    private lateinit var underTest: GetUrlRegexPatternTypeUseCase
    private val isUrlMatchesRegexUseCase = IsUrlMatchesRegexUseCase()
    private val isUrlWhitelistedUseCase = IsUrlWhitelistedUseCase()

    @BeforeEach
    fun init() {
        underTest = GetUrlRegexPatternTypeUseCase(
            isUrlMatchesRegexUseCase,
            isUrlWhitelistedUseCase
        )
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("regexType")
    fun `test that when url does matches regex pattern should return correct regex pattern type`(
        name: String,
        urlToCheck: String?,
        pattern: RegexPatternType,
    ) {
        assertThat(underTest(urlToCheck)).isEqualTo(pattern)
    }

    private fun regexType(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "File Link",
            "https://mega.nz/file/xMpQjZhK#Jp3KceNZRvNsqp",
            RegexPatternType.FILE_LINK
        ),
        Arguments.of(
            "Confirm Link",
            "https://mega.nz/confirm/xMpQjZhK#Jp3KceNZRvNsqp",
            RegexPatternType.CONFIRMATION_LINK
        ),
        Arguments.of(
            "Folder Link",
            "https://mega.nz/folder/xMpQjZhK#Jp3KceNZRvNsqp",
            RegexPatternType.FOLDER_LINK
        ),
        Arguments.of(
            "Chat Link",
            "https://mega.nz/fm/chat/sffzrasd123",
            RegexPatternType.CHAT_LINK
        ),
        Arguments.of("Play Store", PLAY_STORE_URL, RegexPatternType.WHITELISTED_URL),
    )
}