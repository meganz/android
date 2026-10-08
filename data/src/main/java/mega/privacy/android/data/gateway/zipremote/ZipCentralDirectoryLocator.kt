package mega.privacy.android.data.gateway.zipremote

/**
 * Where the central directory of an archive sits, as absolute offsets into the archive.
 */
internal data class CentralDirectoryLocation(val offset: Long, val size: Long)

/**
 * Finds the central directory from the end of an archive, which is all a remote archive needs to
 * be listed: the directory is at the end, and the end record says where it starts.
 */
internal object ZipCentralDirectoryLocator {

    /**
     * The end record is 22 bytes followed by a comment of at most 65535 bytes.
     */
    const val TAIL_BYTES: Long = 22L + 0xFFFF

    const val ZIP64_RECORD_BYTES: Int = 56

    /**
     * The central directory an end record points at, or null when [tail] holds no end record or
     * the record's fields are saturated, which means the real values are in a ZIP64 record.
     *
     * The offsets in the record are absolute, so [tail] can be the whole archive or only its last
     * [TAIL_BYTES].
     */
    fun locate(tail: ByteArray): CentralDirectoryLocation? {
        val eocd = endRecordIndex(tail) ?: return null
        val size = tail.uint32At(eocd + 12)
        val offset = tail.uint32At(eocd + 16)
        if (size == UINT32_MAX || offset == UINT32_MAX) return null
        return CentralDirectoryLocation(offset = offset, size = size)
    }

    /**
     * The absolute offset of the ZIP64 end record, or null when there is no ZIP64 locator in front
     * of the end record.
     */
    fun zip64RecordOffset(tail: ByteArray): Long? {
        val eocd = endRecordIndex(tail) ?: return null
        val locator = eocd - 20
        if (locator < 0 || tail.int32At(locator) != ZIP64_LOCATOR_SIGNATURE) return null
        return tail.int64At(locator + 8)
    }

    /**
     * The central directory a ZIP64 end record points at, or null when [record] is not one.
     */
    fun locateZip64(record: ByteArray): CentralDirectoryLocation? {
        if (record.size < ZIP64_RECORD_BYTES || record.int32At(0) != ZIP64_EOCD_SIGNATURE) {
            return null
        }
        return CentralDirectoryLocation(offset = record.int64At(48), size = record.int64At(40))
    }

    /**
     * Scans backwards and accepts a signature only where the comment length it declares reaches
     * exactly the end of the data, so the same four bytes inside a comment are not taken for it.
     */
    private fun endRecordIndex(tail: ByteArray): Int? =
        (tail.size - 22 downTo 0).firstOrNull { i ->
            tail.int32At(i) == EOCD_SIGNATURE && i + 22 + tail.uint16At(i + 20) == tail.size
        }

    private fun ByteArray.uint16At(i: Int): Int =
        (this[i].toInt() and 0xFF) or ((this[i + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.int32At(i: Int): Int =
        uint16At(i) or (uint16At(i + 2) shl 16)

    private fun ByteArray.uint32At(i: Int): Long = int32At(i).toLong() and 0xFFFFFFFFL

    private fun ByteArray.int64At(i: Int): Long = uint32At(i) or (uint32At(i + 4) shl 32)

    private const val EOCD_SIGNATURE = 0x06054b50
    private const val ZIP64_LOCATOR_SIGNATURE = 0x07064b50
    private const val ZIP64_EOCD_SIGNATURE = 0x06064b50
    private const val UINT32_MAX = 0xFFFFFFFFL
}
