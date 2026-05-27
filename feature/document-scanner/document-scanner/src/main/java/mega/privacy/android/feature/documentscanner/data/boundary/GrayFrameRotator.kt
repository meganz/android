package mega.privacy.android.feature.documentscanner.data.boundary

import javax.inject.Inject

/**
 * Rotates a single-channel (Y plane) frame in 90-degree increments to align
 * with the device's display orientation before downstream processing.
 *
 * CameraX delivers analysis frames in the sensor's native orientation, which
 * is usually 90 degrees off from how the user is holding the phone. The
 * detector expects display-oriented frames so its quad corners come out in a
 * coordinate space the UI can consume without further rotation.
 *
 * The output is a fresh [ByteArray] sized to match the rotated dimensions;
 * the original bytes are not modified. For [degrees] = 0 the input is
 * returned as-is to avoid an unnecessary allocation on the no-op path.
 */
class GrayFrameRotator @Inject constructor() {

    /**
     * Not intended for structural equality: as a data class holding a
     * [ByteArray], the generated `equals`/`hashCode` compare [bytes] by
     * reference, not by content. Callers needing to compare frames should
     * compare [bytes] explicitly (e.g. `contentEquals`).
     */
    data class RotatedFrame(
        val bytes: ByteArray,
        val width: Int,
        val height: Int,
    )

    fun rotate(
        bytes: ByteArray,
        width: Int,
        height: Int,
        degrees: Int,
    ): RotatedFrame = when (degrees) {
        90 -> rotate90(bytes, width, height)
        180 -> rotate180(bytes, width, height)
        270 -> rotate270(bytes, width, height)
        else -> RotatedFrame(bytes, width, height)
    }

    private fun rotate90(bytes: ByteArray, width: Int, height: Int): RotatedFrame {
        val out = ByteArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            out[x * height + (height - 1 - y)] = bytes[y * width + x]
        }
        return RotatedFrame(out, height, width)
    }

    private fun rotate180(bytes: ByteArray, width: Int, height: Int): RotatedFrame {
        val out = ByteArray(width * height)
        for (i in bytes.indices) out[bytes.size - 1 - i] = bytes[i]
        return RotatedFrame(out, width, height)
    }

    private fun rotate270(bytes: ByteArray, width: Int, height: Int): RotatedFrame {
        val out = ByteArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            out[(width - 1 - x) * height + y] = bytes[y * width + x]
        }
        return RotatedFrame(out, height, width)
    }
}
