package mega.privacy.android.app.presentation.settings.exportrecoverykey

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.destination.LegacyExportRecoveryKeyNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExportRecoveryKeyDeepLinkHandlerTest {
    private lateinit var underTest: ExportRecoveryKeyDeepLinkHandler

    @BeforeAll
    fun setup() {
        underTest = ExportRecoveryKeyDeepLinkHandler()
    }

    @Test
    fun `test that correct nav key is returned when the uri matches regex pattern type`() =
        runTest {
            val uriString = "https://mega.co/exportrecoverykey"
            val expected = LegacyExportRecoveryKeyNavKey
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.EXPORT_MASTER_KEY_LINK, true)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when the uri does not match regex pattern type`() =
        runTest {
            val uriString = "https://mega.co/exportrecoverykey"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK, true)

            assertThat(actual).isNull()
        }
}

