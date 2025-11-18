package mega.privacy.android.app.myAccount.navigation


import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.destination.MyAccountNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyAccountDeepLinkHandlerTest {

    private lateinit var underTest: MyAccountDeepLinkHandler


    @BeforeAll
    fun setup() {
        underTest = MyAccountDeepLinkHandler()
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

            val actual = underTest.getNavKeys(uri, RegexPatternType.CANCEL_ACCOUNT_LINK)

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

            val actual = underTest.getNavKeys(uri, RegexPatternType.VERIFY_CHANGE_MAIL_LINK)

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

            val actual = underTest.getNavKeys(uri, RegexPatternType.RESET_PASSWORD_LINK)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when uri does not match ACTION_CANCEL_ACCOUNT, VERIFY_CHANGE_MAIL_LINK or RESET_PASSWORD_LINK pattern type`() =
        runTest {
            val uriString = "mega://other-link"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK)

            assertThat(actual).isNull()
        }
}