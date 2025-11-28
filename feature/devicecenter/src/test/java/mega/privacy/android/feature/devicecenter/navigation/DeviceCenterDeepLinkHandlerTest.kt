package mega.privacy.android.feature.devicecenter.navigation

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.DeviceCenterNavKey
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
class DeviceCenterDeepLinkHandlerTest {
    private lateinit var underTest: DeviceCenterDeepLinkHandler

    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    @BeforeAll
    fun setup() {
        underTest = DeviceCenterDeepLinkHandler(snackbarEventQueue)
    }

    @BeforeEach
    fun resetMocks() {
        reset(snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches OPEN_DEVICE_CENTER_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://device-center"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.OPEN_DEVICE_CENTER_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(DeviceCenterNavKey)
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when uri does not match any handled pattern type`(
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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when regex pattern type is null`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://unknown-link"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, null, isLoggedIn)

        assertThat(actual).isNull()
        verifyNoInteractions(snackbarEventQueue)
    }
}

