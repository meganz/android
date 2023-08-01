package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.utils.APP_STORE_URL
import mega.privacy.android.app.utils.PLAY_STORE_URL
import mega.privacy.android.domain.usecase.IsUrlWhitelistedUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.mega.privacy.android.app.TimberJUnit5Extension
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TimberJUnit5Extension::class)
class IsUrlWhitelistedUseCaseTest {
    private lateinit var underTest: IsUrlWhitelistedUseCase

    @BeforeEach
    fun setup() {
        underTest = IsUrlWhitelistedUseCase()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("acceptedUrl")
    fun `test that when url whitelisted should return true`(
        urlToCheck: String,
    ) {
        assertThat(underTest(urlToCheck)).isTrue()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("blockedUrl")
    fun `test that when url not whitelisted should return false`(
        urlToCheck: String,
    ) {
        assertThat(underTest(urlToCheck)).isFalse()
    }

    private fun acceptedUrl(): Stream<Arguments> = Stream.of(
        Arguments.of(PLAY_STORE_URL),
        Arguments.of(APP_STORE_URL)
    )

    private fun blockedUrl(): Stream<Arguments> = Stream.of(
        Arguments.of("https://play.google.com/store/apps/details?id=mega.privacy.android.app"),
    )
}