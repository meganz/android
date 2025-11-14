package mega.privacy.android.app.myAccount.navigation


import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.navigation.destination.MyAccountNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyAccountDeepLinkHandlerTest {

    private lateinit var underTest: MyAccountDeepLinkHandler

    private val getDecodedUrlRegexPatternTypeUseCase = mock<GetDecodedUrlRegexPatternTypeUseCase>()


    @BeforeAll
    fun setup() {
        underTest = MyAccountDeepLinkHandler(getDecodedUrlRegexPatternTypeUseCase)
    }

    @BeforeEach
    fun cleanUp() {
        reset(getDecodedUrlRegexPatternTypeUseCase)
    }

    @Test
    fun `test that correct nav key is returned when uri matches ACTION_CANCEL_ACCOUNT pattern type`() =
        runTest {
            val uriString = "mega://cancelAccount"
            val expected = MyAccountNavKey(
                action = Constants.ACTION_CANCEL_ACCOUNT,
                link = uriString
            )
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn RegexPatternType.CANCEL_ACCOUNT_LINK

            val actual = underTest.getNavKeysFromUri(uri)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that correct nav key is returned when uri matches VERIFY_CHANGE_MAIL_LINK pattern type`() =
        runTest {
            val uriString = "mega://verifyEmail"
            val expected = MyAccountNavKey(
                action = Constants.ACTION_CHANGE_MAIL,
                link = uriString
            )
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn RegexPatternType.VERIFY_CHANGE_MAIL_LINK

            val actual = underTest.getNavKeysFromUri(uri)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that correct nav key is returned when uri matches RESET_PASSWORD_LINK pattern type`() =
        runTest {
            val uriString = "mega://resetPassword"
            val expected = MyAccountNavKey(
                action = Constants.ACTION_RESET_PASS,
                link = uriString
            )
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn RegexPatternType.RESET_PASSWORD_LINK

            val actual = underTest.getNavKeysFromUri(uri)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when uri does not match ACTION_CANCEL_ACCOUNT, VERIFY_CHANGE_MAIL_LINK or RESET_PASSWORD_LINK pattern type`() =
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