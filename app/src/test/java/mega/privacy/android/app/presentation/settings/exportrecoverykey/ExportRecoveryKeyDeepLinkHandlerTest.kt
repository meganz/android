package mega.privacy.android.app.presentation.settings.exportrecoverykey

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.navigation.destination.LegacyExportRecoveryKeyNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExportRecoveryKeyDeepLinkHandlerTest {
    private lateinit var underTest: ExportRecoveryKeyDeepLinkHandler

    private val getDecodedUrlRegexPatternTypeUseCase = mock<GetDecodedUrlRegexPatternTypeUseCase>()

    @BeforeAll
    fun setup() {
        underTest = ExportRecoveryKeyDeepLinkHandler(getDecodedUrlRegexPatternTypeUseCase)
    }

    @BeforeEach
    fun cleanUp() {
        reset(getDecodedUrlRegexPatternTypeUseCase)
    }

    @Test
    fun `test that correct nav key is returned when the uri matches regex pattern type`() =
        runTest {
            val uriString = "https://mega.co/exportrecoverykey"
            val expected = LegacyExportRecoveryKeyNavKey
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn RegexPatternType.EXPORT_MASTER_KEY_LINK

            val actual = underTest.getNavKeysFromUri(uri)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when the uri does not match regex pattern type`() =
        runTest {
            val uriString = "https://mega.co/exportrecoverykey"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            whenever(getDecodedUrlRegexPatternTypeUseCase(uriString)) doReturn RegexPatternType.FILE_LINK

            val actual = underTest.getNavKeysFromUri(uri)

            assertThat(actual).isNull()
        }
}

