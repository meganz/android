package mega.privacy.android.app.presentation.login.createaccount

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateAccountDeepLinkHandlerTest {
    private lateinit var underTest: CreateAccountDeepLinkHandler

    @BeforeAll
    fun setup() {
        underTest = CreateAccountDeepLinkHandler(mock())
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches REGISTRATION_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://register"
        val expected = CreateAccountNavKey()
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest
            .getNavKeysInternal(uri, RegexPatternType.REGISTRATION_LINK, isLoggedIn)

        assertThat(actual).containsExactly(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when uri does not match REGISTRATION_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://other-link"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest
            .getNavKeysInternal(uri, RegexPatternType.FILE_LINK, isLoggedIn)

        assertThat(actual).isNull()
    }
}

