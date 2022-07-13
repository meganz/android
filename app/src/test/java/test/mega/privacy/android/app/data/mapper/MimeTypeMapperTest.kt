package test.mega.privacy.android.app.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.data.mapper.MimeTypeMapper
import mega.privacy.android.app.data.mapper.getMimeType
import org.junit.Test

class MimeTypeMapperTest {
    private val underTest: MimeTypeMapper = ::getMimeType

    @Test
    fun `test that extensions handled by default mapper are returned`() {
        val expectedType = "expectedType"
        assertThat(underTest("extension") { expectedType }).isEqualTo(expectedType)
    }

    @Test
    fun `test that mkv extension is mapped to video x matroska`() {
        val expectedType = "video/x-matroska"
        assertThat(underTest("mkv") { null }).isEqualTo(expectedType)
    }

    @Test
    fun `test that heic extension is mapped to image heic`() {
        val expectedType = "image/heic"
        assertThat(underTest("heic") { null }).isEqualTo(expectedType)
    }

    @Test
    fun `test that url extension is mapped to web url`() {
        val expectedType = "web/url"
        assertThat(underTest("url") { null }).isEqualTo(expectedType)
    }

    @Test
    fun `test that webp extension is mapped to image webp`() {
        val expectedType = "image/webp"
        assertThat(underTest("webp") { null }).isEqualTo(expectedType)
    }

    @Test
    fun `test that unknown extension is mapped to application octet-stream`() {
        val expectedType = "application/octet-stream"
        assertThat(underTest("unknown extension") { null }).isEqualTo(expectedType)
    }

    @Test
    fun `test that extension is lower cased`() {
        val expectedType = "expectedType"
        val lowerCaseMatcher =
            { ext: String -> if (ext.equals(ext.lowercase(), false)) expectedType else null }
        assertThat(underTest("extension".uppercase(), lowerCaseMatcher)).isEqualTo(expectedType)
    }
}