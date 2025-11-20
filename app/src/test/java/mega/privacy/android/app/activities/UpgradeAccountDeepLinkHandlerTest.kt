package mega.privacy.android.app.activities

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.shared.resources.R as sharedR
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
class UpgradeAccountDeepLinkHandlerTest {
    private lateinit var underTest: UpgradeAccountDeepLinkHandler

    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    @BeforeAll
    fun setup() {
        underTest = UpgradeAccountDeepLinkHandler(snackbarEventQueue = snackbarEventQueue)
    }

    @BeforeEach
    fun resetMocks() {
        reset(snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches UPGRADE_PAGE_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.app/upgrade"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.UPGRADE_PAGE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(UpgradeAccountNavKey())
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches UPGRADE_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.app/pro"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.UPGRADE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(UpgradeAccountNavKey())
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when uri does not match upgrade pattern types`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.app/other-link"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK, isLoggedIn)

        assertThat(actual).isNull()
        verifyNoInteractions(snackbarEventQueue)
    }
}

