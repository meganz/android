package mega.privacy.android.app.presentation.filelink

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.destination.LegacyFileLinkNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileLinkDeepLinkHandlerTest {
    private lateinit var underTest: FileLinkDeepLinkHandler

    @BeforeAll
    fun setup() {
        underTest = FileLinkDeepLinkHandler(mock())
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when the uri matches regex pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.co/file"
        val expected = LegacyFileLinkNavKey(uriString)
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeysInternal(uri, RegexPatternType.FILE_LINK, isLoggedIn)

        assertThat(actual).containsExactly(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when the uri does not match regex pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.co/file"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeysInternal(uri, RegexPatternType.FOLDER_LINK, isLoggedIn)

        assertThat(actual).isNull()
    }
}
