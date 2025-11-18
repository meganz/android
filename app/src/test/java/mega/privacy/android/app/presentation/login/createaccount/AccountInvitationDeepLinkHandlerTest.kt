package mega.privacy.android.app.presentation.login.createaccount

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountInvitationDeepLinkHandlerTest {
    private lateinit var underTest: AccountInvitationDeepLinkHandler

    private val querySignupLinkUseCase = mock<QuerySignupLinkUseCase>()

    @BeforeAll
    fun setup() {
        underTest = AccountInvitationDeepLinkHandler(
            querySignupLinkUseCase = querySignupLinkUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(querySignupLinkUseCase)
    }

    @Test
    fun `test that correct nav key is returned when regex pattern type is ACCOUNT_INVITATION_LINK`() =
        runTest {
            val uriString = "https://mega.co.nz/#newsignup"
            val expectedEmail = "test@example.com"
            val expectedNavKey = CreateAccountNavKey(initialEmail = expectedEmail)
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            whenever(querySignupLinkUseCase(uriString)) doReturn expectedEmail

            val actual = underTest.getNavKeys(
                regexPatternType = RegexPatternType.ACCOUNT_INVITATION_LINK,
                uri = uri,
            )

            assertThat(actual).containsExactly(expectedNavKey)
        }

    @Test
    fun `test that null is returned when regex pattern type is not ACCOUNT_INVITATION_LINK`() =
        runTest {
            val uriString = "https://mega.co.nz/#login"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(
                regexPatternType = RegexPatternType.LOGIN_LINK,
                uri = uri,
            )

            assertThat(actual).isNull()
        }

    @Test
    fun `test that correct nav key is returned when get email throws an exception`() =
        runTest {
            val uriString = "https://mega.co.nz/#newsignup"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            whenever(querySignupLinkUseCase(uriString)) doThrow RuntimeException("something bad")

            val actual = underTest.getNavKeys(
                regexPatternType = RegexPatternType.ACCOUNT_INVITATION_LINK,
                uri = uri,
            )

            assertThat(actual).containsExactly(CreateAccountNavKey(initialEmail = null))
        }
}

