package mega.privacy.android.app.presentation.settings

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.ENABLE_CAMERA_UPLOADS_LINK
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsDeepLinkHandlerTest {
    private lateinit var underTest: SettingsDeepLinkHandler

    @BeforeAll
    fun setup() {
        underTest = SettingsDeepLinkHandler()
    }

    @Test
    fun `test that correct nav key is returned when uri matches ENABLE_CAMERA_UPLOADS_LINK pattern type`() =
        runTest {
            val uriString = "https://mega.nz/settings/camera"
            val expected = SettingsCameraUploadsNavKey
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, ENABLE_CAMERA_UPLOADS_LINK, true)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when uri does not match ENABLE_CAMERA_UPLOADS_LINK pattern type`() =
        runTest {
            val uriString = "https://other-link"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK, true)

            assertThat(actual).isNull()
        }
}

