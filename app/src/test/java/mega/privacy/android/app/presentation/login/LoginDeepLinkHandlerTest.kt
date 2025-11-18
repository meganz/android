package mega.privacy.android.app.presentation.login

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginDeepLinkHandlerTest {
    private lateinit var underTest: LoginDeepLinkHandler

    @BeforeAll
    fun setup() {
        underTest = LoginDeepLinkHandler()
    }

    @Test
    fun `test that correct nav key is returned when uri matches LOGIN_LINK pattern type`() =
        runTest {
            val uriString = "mega://login"
            val expected = LoginNavKey()
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.LOGIN_LINK)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when uri does not match LOGIN_LINK pattern type`() =
        runTest {
            val uriString = "mega://other-link"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK)

            assertThat(actual).isNull()
        }
}

