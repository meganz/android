package test.mega.privacy.android.app

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.ShareInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareInfoTest {
    private lateinit var underTest: ShareInfo
    private var uri: Uri = mock()

    @BeforeEach
    fun init() {
        underTest = ShareInfo()
    }

    @BeforeEach
    fun resetMocks() {
        reset(uri)
    }

    @ParameterizedTest(name = "Uri type: {0}")
    @MethodSource("malformedUriParameters")
    fun `test that uri should not be processed when uri path points to mega local path`(
        uriType: String,
        uriPath: String,
    ) {
        whenever(uri.path).thenReturn(uriPath)

        val isUriProcessed = underTest.processUri(uri, mock())

        assertThat(isUriProcessed).isFalse()
        assertThat(uri.path).isEqualTo(uriPath)
    }

    private fun malformedUriParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Malformed",
            "file:///data/data/ mega.privacy.android.app/app_webview/Default/Cookies"
        ),
        Arguments.of(
            "Cookies",
            "file:///data/data/mega.privacy.android.app/app_webview/Default/Cookies"
        ),
        Arguments.of(
            "Shared Pref",
            "file:///data/data/mega.privacy.android.app/shared_prefs/secrets.xml"
        ),
        Arguments.of(
            "Private Dir",
            "file:///data/user/0/mega.privacy.android.app/shared_prefs/secrets.xml"
        ),
    )
}