package mega.privacy.android.app.presentation.settings

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.ENABLE_CAMERA_UPLOADS_LINK
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsDeepLinkHandlerTest {
    private lateinit var underTest: SettingsDeepLinkHandler

    private val getDecodedUrlRegexPatternTypeUseCase = mock<GetDecodedUrlRegexPatternTypeUseCase>()

    @BeforeAll
    fun setup() {
        underTest = SettingsDeepLinkHandler(getDecodedUrlRegexPatternTypeUseCase)
    }

    @BeforeEach
    fun cleanUp() {
        reset(getDecodedUrlRegexPatternTypeUseCase)
    }

    @Test
    fun `test that correct nav key is returned when uri matches ENABLE_CAMERA_UPLOADS_LINK pattern type`() =
        runTest {
            val uriString = "https://mega.nz/settings/camera"
            val expected = SettingsCameraUploadsNavKey
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn ENABLE_CAMERA_UPLOADS_LINK

            val actual = underTest.getNavKeysFromUri(uri)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when uri does not match ENABLE_CAMERA_UPLOADS_LINK pattern type`() =
        runTest {
            val uriString = "https://other-link"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn RegexPatternType.FILE_LINK

            val actual = underTest.getNavKeysFromUri(uri)

            assertThat(actual).isNull()
        }
}

