package mega.privacy.android.app.activities.navigation

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.link.GetSessionLinkUseCase
import mega.privacy.android.navigation.destination.WebSiteNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebViewDeepLinkHandlerTest {

    private lateinit var underTest: WebViewDeepLinkHandler

    private val getDecodedUrlRegexPatternTypeUseCase = mock<GetDecodedUrlRegexPatternTypeUseCase>()
    private val getSessionLinkUseCase = mock<GetSessionLinkUseCase>()

    @BeforeAll
    fun setup() {
        underTest = WebViewDeepLinkHandler(
            getDecodedUrlRegexPatternTypeUseCase = getDecodedUrlRegexPatternTypeUseCase,
            getSessionLinkUseCase = getSessionLinkUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getDecodedUrlRegexPatternTypeUseCase, getSessionLinkUseCase)
    }

    @ParameterizedTest
    @EnumSource(
        value = RegexPatternType::class,
        names = ["EMAIL_VERIFY_LINK", "WEB_SESSION_LINK", "BUSINESS_INVITE_LINK", "MEGA_DROP_LINK",
            "MEGA_FILE_REQUEST_LINK", "REVERT_CHANGE_PASSWORD_LINK", "INSTALLER_DOWNLOAD_LINK",
            "MEGA_BLOG_LINK", "PURCHASE_LINK"]
    )
    fun `test that correct nav key is returned when the uri matches regex pattern type`(
        regexPatternType: RegexPatternType,
    ) = runTest {
        val uriString = "https://mega.app/whatever"
        val expected = WebSiteNavKey(uriString)
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn regexPatternType

        assertThat(underTest.getNavKeysFromUri(uri)).containsExactly(expected)
    }

    @Test
    fun `test that null is returned when the uri does not match regex pattern type`() =
        runTest {
            val uriString = "https://mega.app/whatever"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn RegexPatternType.FOLDER_LINK

            assertThat(underTest.getNavKeysFromUri(uri)).isNull()
        }

    @Test
    fun `test that correct nav key is returned if link requires session`() = runTest {
        val uriString = "https://mega.app/whatever"
        val sessionUriString = "https://mega.app/withSession"
        val expected = WebSiteNavKey(sessionUriString)
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn RegexPatternType.MEGA_LINK
        whenever(getSessionLinkUseCase(uriString)) doReturn sessionUriString

        assertThat(underTest.getNavKeysFromUri(uri)).containsExactly(expected)
    }

    @Test
    fun `test that correct nav key is returned if link does not require session but is a MEGA_LINK`() =
        runTest {
            val uriString = "https://mega.app/whatever"
            val expected = WebSiteNavKey(uriString)
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn RegexPatternType.MEGA_LINK
            whenever(getSessionLinkUseCase(uriString)) doReturn null

            assertThat(underTest.getNavKeysFromUri(uri)).containsExactly(expected)
        }
}