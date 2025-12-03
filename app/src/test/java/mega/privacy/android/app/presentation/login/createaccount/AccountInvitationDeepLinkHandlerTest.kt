package mega.privacy.android.app.presentation.login.createaccount

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountInvitationDeepLinkHandlerTest {
    private lateinit var underTest: AccountInvitationDeepLinkHandler

    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    private val querySignupLinkUseCase = mock<QuerySignupLinkUseCase>()

    @BeforeAll
    fun setup() {
        underTest = AccountInvitationDeepLinkHandler(
            querySignupLinkUseCase = querySignupLinkUseCase,
            snackbarEventQueue = snackbarEventQueue,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(querySignupLinkUseCase, snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when regex pattern type is ACCOUNT_INVITATION_LINK`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.co.nz/#newsignup"
        val expectedEmail = "test@example.com"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(querySignupLinkUseCase(uriString)) doReturn expectedEmail

        val actual = underTest.getNavKeysInternal(
            regexPatternType = RegexPatternType.ACCOUNT_INVITATION_LINK,
            uri = uri,
            isLoggedIn = isLoggedIn,
        )

        if (isLoggedIn) {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(R.string.log_out_warning)
        } else {
            assertThat(actual).containsExactly(CreateAccountNavKey(initialEmail = expectedEmail))
            verifyNoInteractions(snackbarEventQueue)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when regex pattern type is not ACCOUNT_INVITATION_LINK`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.co.nz/#login"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeysInternal(
            regexPatternType = RegexPatternType.LOGIN_LINK,
            uri = uri,
            isLoggedIn = isLoggedIn,
        )

        assertThat(actual).isNull()
        verifyNoInteractions(snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when get email throws an exception`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.co.nz/#newsignup"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(querySignupLinkUseCase(uriString)) doThrow RuntimeException("something bad")

        val actual = underTest.getNavKeysInternal(
            regexPatternType = RegexPatternType.ACCOUNT_INVITATION_LINK,
            uri = uri,
            isLoggedIn = isLoggedIn,
        )

        if (isLoggedIn) {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(R.string.log_out_warning)
        } else {
            assertThat(actual).containsExactly(CreateAccountNavKey(initialEmail = null))
            verifyNoInteractions(snackbarEventQueue)
        }
    }
}

