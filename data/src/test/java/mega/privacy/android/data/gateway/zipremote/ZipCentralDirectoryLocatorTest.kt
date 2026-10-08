package mega.privacy.android.data.gateway.zipremote

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipCentralDirectoryLocatorTest {

    @Test
    fun `test that locate finds the central directory of a plain archive`() {
        val zip = archive(comment = null)

        val location = ZipCentralDirectoryLocator.locate(zip)

        assertThat(location).isNotNull()
        assertThat(zip.int32At(location!!.offset.toInt())).isEqualTo(CENTRAL_HEADER_SIGNATURE)
        assertThat(location.offset + location.size).isEqualTo(eocdOffsetOf(zip).toLong())
    }

    @Test
    fun `test that locate finds the central directory when the archive has a comment`() {
        val zip = archive(comment = "PK\u0005\u0006 inside the comment must not confuse it")

        val location = ZipCentralDirectoryLocator.locate(zip)

        assertThat(zip.int32At(location!!.offset.toInt())).isEqualTo(CENTRAL_HEADER_SIGNATURE)
    }

    @Test
    fun `test that locate returns the same absolute offsets from the tail alone`() {
        val zip = archive(comment = null)
        val tailStart = 10L
        val tail = zip.copyOfRange(tailStart.toInt(), zip.size)

        val fromTail = ZipCentralDirectoryLocator.locate(tail)

        assertThat(fromTail).isEqualTo(ZipCentralDirectoryLocator.locate(zip))
    }

    @Test
    fun `test that locate returns null when there is no end of central directory record`() {
        assertThat(ZipCentralDirectoryLocator.locate(ByteArray(100))).isNull()
    }

    @Test
    fun `test that zip64RecordOffset reads the locator when the plain record is saturated`() {
        val tail = zip64Tail(zip64RecordOffset = 5_000_000_000L)

        assertThat(ZipCentralDirectoryLocator.locate(tail)).isNull()
        assertThat(ZipCentralDirectoryLocator.zip64RecordOffset(tail))
            .isEqualTo(5_000_000_000L)
    }

    @Test
    fun `test that locateZip64 reads the 64 bit offset and size`() {
        val record = ByteArray(56).apply {
            putInt32(0, ZIP64_EOCD_SIGNATURE)
            putInt64(40, 123_456L)
            putInt64(48, 6_000_000_000L)
        }

        assertThat(ZipCentralDirectoryLocator.locateZip64(record))
            .isEqualTo(CentralDirectoryLocation(offset = 6_000_000_000L, size = 123_456L))
    }

    private fun archive(comment: String?): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            repeat(3) { i ->
                zip.putNextEntry(ZipEntry("folder/file_$i.txt"))
                zip.write("content $i".toByteArray())
                zip.closeEntry()
            }
            comment?.let(zip::setComment)
        }
        return out.toByteArray()
    }

    private fun eocdOffsetOf(zip: ByteArray): Int =
        (zip.size - 22 downTo 0).first { zip.int32At(it) == EOCD_SIGNATURE }

    private fun zip64Tail(zip64RecordOffset: Long): ByteArray = ByteArray(20 + 22).apply {
        putInt32(0, ZIP64_LOCATOR_SIGNATURE)
        putInt64(8, zip64RecordOffset)
        putInt32(20, EOCD_SIGNATURE)
        putInt32(20 + 12, -1)
        putInt32(20 + 16, -1)
    }

    private fun ByteArray.int32At(i: Int): Int =
        (this[i].toInt() and 0xFF) or ((this[i + 1].toInt() and 0xFF) shl 8) or
                ((this[i + 2].toInt() and 0xFF) shl 16) or ((this[i + 3].toInt() and 0xFF) shl 24)

    private fun ByteArray.putInt32(i: Int, v: Int) {
        for (b in 0 until 4) this[i + b] = (v ushr (8 * b)).toByte()
    }

    private fun ByteArray.putInt64(i: Int, v: Long) {
        for (b in 0 until 8) this[i + b] = (v ushr (8 * b)).toByte()
    }

    private companion object {
        const val EOCD_SIGNATURE = 0x06054b50
        const val CENTRAL_HEADER_SIGNATURE = 0x02014b50
        const val ZIP64_LOCATOR_SIGNATURE = 0x07064b50
        const val ZIP64_EOCD_SIGNATURE = 0x06064b50
    }
}
