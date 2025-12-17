package mega.privacy.android.app.activities.navigation

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.link.GetSessionLinkUseCase
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.DeepLinksAfterFetchNodesDialogNavKey
import mega.privacy.android.navigation.destination.WebSiteNavKey
import mega.privacy.android.shared.resources.R
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebViewDeepLinkHandlerTest {

    private lateinit var underTest: WebViewDeepLinkHandler

    private val getSessionLinkUseCase = mock<GetSessionLinkUseCase>()
    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    @BeforeAll
    fun setup() {
        underTest = WebViewDeepLinkHandler(
            getSessionLinkUseCase = getSessionLinkUseCase,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            snackbarEventQueue = snackbarEventQueue,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getSessionLinkUseCase,
            rootNodeExistsUseCase,
            snackbarEventQueue,
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = RegexPatternType::class,
        names = ["EMAIL_VERIFY_LINK", "WEB_SESSION_LINK", "BUSINESS_INVITE_LINK", "MEGA_DROP_LINK",
            "MEGA_FILE_REQUEST_LINK", "REVERT_CHANGE_PASSWORD_LINK", "INSTALLER_DOWNLOAD_LINK",
            "MEGA_BLOG_LINK", "PURCHASE_LINK"]
    )
    fun `test that correct nav key is returned when the uri matches regex pattern type when logged in`(
        regexPatternType: RegexPatternType?,
    ) = runTest {
        val uriString = "https://mega.app/whatever"
        val expected = WebSiteNavKey(uriString)
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        assertThat(underTest.getNavKeysInternal(uri, regexPatternType, true))
            .containsExactly(expected)
    }

    @ParameterizedTest
    @EnumSource(
        value = RegexPatternType::class,
        names = ["EMAIL_VERIFY_LINK", "WEB_SESSION_LINK", "BUSINESS_INVITE_LINK", "MEGA_DROP_LINK",
            "MEGA_FILE_REQUEST_LINK", "REVERT_CHANGE_PASSWORD_LINK", "INSTALLER_DOWNLOAD_LINK",
            "MEGA_BLOG_LINK", "PURCHASE_LINK"]
    )
    fun `test that correct nav key is returned when the uri matches regex pattern type when logged out`(
        regexPatternType: RegexPatternType?,
    ) = runTest {
        val uriString = "https://mega.app/whatever"
        val expected = WebSiteNavKey(uriString)
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        assertThat(underTest.getNavKeysInternal(uri, regexPatternType, false))
            .containsExactly(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when the uri does not match regex pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.app/whatever"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        assertThat(underTest.getNavKeysInternal(uri, RegexPatternType.FOLDER_LINK, isLoggedIn))
            .isNull()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned if link requires session and root node does not exist`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.app/fm/whatever"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(rootNodeExistsUseCase()) doReturn false

        val actual =
            assertThat(underTest.getNavKeysInternal(uri, RegexPatternType.MEGA_LINK, isLoggedIn))

        if (isLoggedIn) {
            actual.containsExactly(
                DeepLinksAfterFetchNodesDialogNavKey(
                    deepLink = uriString,
                    regexPatternType = RegexPatternType.MEGA_LINK
                )
            )
        } else {
            actual.isEmpty()
            verify(snackbarEventQueue).queueMessage(R.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned if link requires session and root node does exist`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.app/fm/whatever"
        val sessionUriString = "https://mega.app/withSession"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(rootNodeExistsUseCase()) doReturn true
        whenever(getSessionLinkUseCase(uriString)) doReturn sessionUriString

        val actual =
            assertThat(underTest.getNavKeysInternal(uri, RegexPatternType.MEGA_LINK, isLoggedIn))

        if (isLoggedIn) {
            actual.containsExactly(WebSiteNavKey(sessionUriString))
        } else {
            actual.isEmpty()
            verify(snackbarEventQueue).queueMessage(R.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned if link requires session and getSessionLinkUseCase throws exception`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.app/fm/whatever"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(rootNodeExistsUseCase()) doReturn true
        whenever(getSessionLinkUseCase(uriString)) doThrow RuntimeException()

        val actual =
            assertThat(underTest.getNavKeysInternal(uri, RegexPatternType.MEGA_LINK, isLoggedIn))

        if (isLoggedIn) {
            actual.containsExactly(WebSiteNavKey(uriString))
        } else {
            actual.isEmpty()
            verify(snackbarEventQueue).queueMessage(R.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned if link does not require session but is a MEGA_LINK`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.app/whatever"
        val expected = WebSiteNavKey(uriString)
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(getSessionLinkUseCase(uriString)) doReturn null

        assertThat(underTest.getNavKeysInternal(uri, RegexPatternType.MEGA_LINK, isLoggedIn))
            .containsExactly(expected)
    }
}