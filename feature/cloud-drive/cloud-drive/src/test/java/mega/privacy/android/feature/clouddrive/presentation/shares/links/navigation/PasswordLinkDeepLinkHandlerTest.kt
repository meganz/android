package mega.privacy.android.feature.clouddrive.presentation.shares.links.navigation

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.feature.clouddrive.presentation.shares.links.OpenPasswordLinkDialogNavKey
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordLinkDeepLinkHandlerTest {
    private lateinit var underTest: PasswordLinkDeepLinkHandler

    private val snackbarEventQueue = mock<SnackbarEventQueue>()


    @BeforeAll
    fun setup() {
        underTest = PasswordLinkDeepLinkHandler(
            snackbarEventQueue = snackbarEventQueue,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches PASSWORD_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.nz/encryptedLink"
        val expected = OpenPasswordLinkDialogNavKey(uriString)
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.PASSWORD_LINK, isLoggedIn)

        assertThat(actual).containsExactly(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when regex pattern type is not PASSWORD_LINK`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://other-link"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK, isLoggedIn)

        assertThat(actual).isNull()
        verifyNoInteractions(snackbarEventQueue)
    }
}

