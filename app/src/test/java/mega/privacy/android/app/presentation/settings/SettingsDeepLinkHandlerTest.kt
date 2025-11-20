package mega.privacy.android.app.presentation.settings

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.ENABLE_CAMERA_UPLOADS_LINK
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
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
class SettingsDeepLinkHandlerTest {
    private lateinit var underTest: SettingsDeepLinkHandler

    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    @BeforeAll
    fun setup() {
        underTest = SettingsDeepLinkHandler(snackbarEventQueue)
    }

    @BeforeEach
    fun resetMocks() {
        reset(snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches ENABLE_CAMERA_UPLOADS_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.nz/settings/camera"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, ENABLE_CAMERA_UPLOADS_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(SettingsCameraUploadsNavKey)
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when uri does not match ENABLE_CAMERA_UPLOADS_LINK pattern type`(
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

