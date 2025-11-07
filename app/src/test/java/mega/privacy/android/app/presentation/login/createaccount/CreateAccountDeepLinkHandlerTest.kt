package mega.privacy.android.app.presentation.login.createaccount

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateAccountDeepLinkHandlerTest {
    private lateinit var underTest: CreateAccountDeepLinkHandler

    private val getDecodedUrlRegexPatternTypeUseCase = mock<GetDecodedUrlRegexPatternTypeUseCase>()

    @BeforeAll
    fun setup() {
        underTest = CreateAccountDeepLinkHandler(getDecodedUrlRegexPatternTypeUseCase)
    }

    @BeforeEach
    fun cleanUp() {
        reset(getDecodedUrlRegexPatternTypeUseCase)
    }

    @Test
    fun `test that correct nav key is returned when uri matches REGISTRATION_LINK pattern type`() =
        runTest {
            val uriString = "mega://register"
            val expected = CreateAccountNavKey
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn RegexPatternType.REGISTRATION_LINK

            val actual = underTest.getNavKeysFromUri(uri)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when uri does not match REGISTRATION_LINK pattern type`() =
        runTest {
            val uriString = "mega://other-link"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn RegexPatternType.FILE_LINK

            val actual = underTest.getNavKeysFromUri(uri)

            assertThat(actual).isNull()
        }
}

