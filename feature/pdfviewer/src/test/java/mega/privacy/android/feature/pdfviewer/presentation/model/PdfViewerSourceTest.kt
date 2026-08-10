package mega.privacy.android.feature.pdfviewer.presentation.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PdfViewerSourceTest {

    @ParameterizedTest
    @ValueSource(strings = ["http://example.com/file.pdf", "https://example.com/file.pdf"])
    fun `test that ExternalFile isRemote returns true when scheme is http or https`(uri: String) {
        val source = PdfViewerSource.ExternalFile(contentUri = uri, fileName = "sample.pdf")

        assertThat(source.isRemote).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["content://authority/file.pdf", "file:///storage/emulated/0/file.pdf"])
    fun `test that ExternalFile isRemote returns false when scheme is content or file`(uri: String) {
        val source = PdfViewerSource.ExternalFile(contentUri = uri, fileName = "sample.pdf")

        assertThat(source.isRemote).isFalse()
    }
}
