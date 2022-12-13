package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Mime type mapper test
 *
 * An official list of mapped mime types can be found [here](https://www.iana.org/assignments/media-types/media-types.xhtml)
 */
class MimeTypeMapperTest {
    private val underTest = ::getMimeType

    private val mimeTypeMap = mapOf(
        "dcr" to "image/dcr",
        "url" to "web/url",
        "3fr" to "image/3fr",
        "iiq" to "image/iiq",
        "k25" to "image/k25",
        "kdc" to "image/kdc",
        "mef" to "image/mef",
        "mos" to "image/mos",
        "mrw" to "image/mrw",
        "raw" to "image/raw",
        "rwl" to "image/rwl",
        "sr2" to "image/sr2",
        "srf" to "image/srf",
        "x3f" to "image/x3f",
        "cr3" to "image/cr3",
        "ciff" to "image/ciff")
    
    @Test
    fun `test that extensions handled by default mapper are returned`() {
        val expectedType = "expectedType"
        assertThat(underTest("extension") { expectedType }).isEqualTo(expectedType)
    }

    @Test
    fun `test that extensions are mapped correctly with custom type when default type is null`() {
        mimeTypeMap.forEach { (key, value) ->
            assertThat(underTest(key) { null }).isEqualTo(value)
        }
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
