package mega.privacy.android.data.mapper

import javax.inject.Inject

/**
 * Detects the MIME type of a file from the first bytes of its content (magic numbers), used to
 * identify files that have no extension. Returns null when the content is not recognised.
 */
internal class FileContentTypeMapper @Inject constructor() {

    /**
     * @param header the first bytes of the file (a header is enough; magic numbers live at the start).
     * @return the detected MIME type, or null when the content cannot be recognised.
     */
    operator fun invoke(header: ByteArray): String? {
        if (header.isEmpty()) return null
        return detectBinaryMimeType(header) ?: "text/plain".takeIf { looksLikeText(header) }
    }

    private fun detectBinaryMimeType(bytes: ByteArray): String? = when {
        bytes.matchAt(0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "image/png"
        bytes.matchAt(0, 0xFF, 0xD8, 0xFF) -> "image/jpeg"
        bytes.matchAt(0, 0x47, 0x49, 0x46, 0x38) -> "image/gif"
        bytes.matchAt(0, 0x42, 0x4D) -> "image/bmp"
        bytes.matchAt(0, 0x49, 0x49, 0x2A, 0x00) || bytes.matchAt(
            0,
            0x4D,
            0x4D,
            0x00,
            0x2A
        ) -> "image/tiff"

        bytes.matchAt(0, 0x25, 0x50, 0x44, 0x46) -> "application/pdf"
        bytes.matchAt(0, 0x1A, 0x45, 0xDF, 0xA3) -> matroskaMimeType(bytes)
        bytes.matchAt(0, 0x46, 0x4C, 0x56) -> "video/x-flv"
        bytes.matchAt(0, 0x00, 0x00, 0x01, 0xBA) || bytes.matchAt(
            0,
            0x00,
            0x00,
            0x01,
            0xB3
        ) -> "video/mpeg"

        bytes.matchAt(0, 0x66, 0x4C, 0x61, 0x43) -> "audio/flac"
        bytes.matchAt(0, 0x4F, 0x67, 0x67, 0x53) -> "audio/ogg"
        bytes.matchAt(0, 0x49, 0x44, 0x33) ||
                (bytes.size > 1 && bytes[0].toInt() and 0xFF == 0xFF && bytes[1].toInt() and 0xE0 == 0xE0) -> "audio/mpeg"

        bytes.matchAt(0, 0x52, 0x49, 0x46, 0x46) -> riffMimeType(bytes)
        bytes.matchAt(4, 0x66, 0x74, 0x79, 0x70) -> ftypMimeType(bytes)
        else -> null
    }

    private fun riffMimeType(bytes: ByteArray): String? = when {
        bytes.matchAt(8, 0x57, 0x45, 0x42, 0x50) -> "image/webp"
        bytes.matchAt(8, 0x57, 0x41, 0x56, 0x45) -> "audio/x-wav"
        bytes.matchAt(8, 0x41, 0x56, 0x49, 0x20) -> "video/x-msvideo"
        else -> null
    }

    private fun matroskaMimeType(bytes: ByteArray): String {
        val doctype = String(bytes, Charsets.ISO_8859_1)
        return if (doctype.contains("webm")) "video/webm" else "video/x-matroska"
    }

    private fun ftypMimeType(bytes: ByteArray): String {
        val brand = if (bytes.size >= 12) {
            String(bytes.copyOfRange(8, 12), Charsets.ISO_8859_1).trim().lowercase()
        } else ""
        return when {
            brand.startsWith("heic") || brand.startsWith("heix") ||
                    brand.startsWith("mif1") || brand.startsWith("msf1") -> "image/heic"

            brand.startsWith("avif") -> "image/avif"
            brand.startsWith("m4a") -> "audio/mp4"
            brand.startsWith("qt") -> "video/quicktime"
            else -> "video/mp4"
        }
    }

    private fun looksLikeText(bytes: ByteArray): Boolean {
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            if (value == 0) return false
            val isPrintableOrWhitespace = value >= 0x20 ||
                    value == 0x09 || value == 0x0A || value == 0x0D || value == 0x0C
            if (!isPrintableOrWhitespace) return false
        }
        return true
    }

    private fun ByteArray.matchAt(offset: Int, vararg signature: Int): Boolean {
        if (offset < 0 || offset + signature.size > size) return false
        return signature.withIndex().all { (index, expected) ->
            this[offset + index].toInt() and 0xFF == expected
        }
    }
}
