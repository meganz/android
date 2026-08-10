package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileContentTypeMapperTest {
    private val underTest = FileContentTypeMapper()

    private fun bytesOf(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun `test that an empty header returns null`() {
        assertThat(underTest(ByteArray(0))).isNull()
    }

    @Test
    fun `test that a png header is detected`() {
        val header = bytesOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01)
        assertThat(underTest(header)).isEqualTo("image/png")
    }

    @Test
    fun `test that a jpeg header is detected`() {
        assertThat(underTest(bytesOf(0xFF, 0xD8, 0xFF, 0xE0))).isEqualTo("image/jpeg")
    }

    @Test
    fun `test that a gif header is detected`() {
        assertThat(underTest(bytesOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61))).isEqualTo("image/gif")
    }

    @Test
    fun `test that a pdf header is detected`() {
        assertThat(underTest(bytesOf(0x25, 0x50, 0x44, 0x46, 0x2D))).isEqualTo("application/pdf")
    }

    @Test
    fun `test that an mp4 ftyp header is detected as video`() {
        val header = bytesOf(
            0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70,
            0x69, 0x73, 0x6F, 0x6D,
        )
        assertThat(underTest(header)).isEqualTo("video/mp4")
    }

    @Test
    fun `test that an m4a ftyp header is detected as audio`() {
        val header = bytesOf(
            0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70,
            0x4D, 0x34, 0x41, 0x20,
        )
        assertThat(underTest(header)).isEqualTo("audio/mp4")
    }

    @Test
    fun `test that a heic ftyp header is detected as image`() {
        val header = bytesOf(
            0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70,
            0x68, 0x65, 0x69, 0x63,
        )
        assertThat(underTest(header)).isEqualTo("image/heic")
    }

    @Test
    fun `test that a webp riff header is detected as image`() {
        val header = bytesOf(
            0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50,
        )
        assertThat(underTest(header)).isEqualTo("image/webp")
    }

    @Test
    fun `test that a wav riff header is detected as audio`() {
        val header = bytesOf(
            0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
            0x57, 0x41, 0x56, 0x45,
        )
        assertThat(underTest(header)).isEqualTo("audio/x-wav")
    }

    @Test
    fun `test that an avi riff header is detected as video`() {
        val header = bytesOf(
            0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
            0x41, 0x56, 0x49, 0x20,
        )
        assertThat(underTest(header)).isEqualTo("video/x-msvideo")
    }

    @Test
    fun `test that a matroska header is detected as video`() {
        assertThat(underTest(bytesOf(0x1A, 0x45, 0xDF, 0xA3, 0x01, 0x02)))
            .isEqualTo("video/x-matroska")
    }

    @Test
    fun `test that an mp3 id3 header is detected as audio`() {
        assertThat(underTest(bytesOf(0x49, 0x44, 0x33, 0x03))).isEqualTo("audio/mpeg")
    }

    @Test
    fun `test that a flac header is detected as audio`() {
        assertThat(underTest(bytesOf(0x66, 0x4C, 0x61, 0x43, 0x00))).isEqualTo("audio/flac")
    }

    @Test
    fun `test that plain text content is detected as text`() {
        val header = "Hello, this is a plain text file without extension.\n".toByteArray()
        assertThat(underTest(header)).isEqualTo("text/plain")
    }

    @Test
    fun `test that content with a null byte is not detected as text`() {
        val header = bytesOf(0x48, 0x65, 0x00, 0x6C, 0x6C, 0x6F)
        assertThat(underTest(header)).isNull()
    }

    @Test
    fun `test that unrecognised binary content returns null`() {
        val header = bytesOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        assertThat(underTest(header)).isNull()
    }
}
