package mega.privacy.android.app.presentation.login

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.ACTION_CONFIRM
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginDeepLinkHandlerTest {
    private lateinit var underTest: LoginDeepLinkHandler

    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    @BeforeAll
    fun setup() {
        underTest = LoginDeepLinkHandler(snackbarEventQueue)
    }

    @BeforeEach
    fun resetMocks() {
        reset(snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches LOGIN_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://login"
        val expected = LoginNavKey()
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.LOGIN_LINK, isLoggedIn)

        assertThat(actual).containsExactly(expected)
        verifyNoInteractions(snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches CONFIRMATION_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://confirmLink"
        val expected = LoginNavKey(action = ACTION_CONFIRM, link = uriString)
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.CONFIRMATION_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(R.string.log_out_warning)
        } else {
            assertThat(actual).containsExactly(expected)
            verifyNoInteractions(snackbarEventQueue)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when uri does not match LOGIN_LINK or CONFIRMATION_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://other-link"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK, isLoggedIn)

        assertThat(actual).isNull()
        verifyNoInteractions(snackbarEventQueue)
    }
}

