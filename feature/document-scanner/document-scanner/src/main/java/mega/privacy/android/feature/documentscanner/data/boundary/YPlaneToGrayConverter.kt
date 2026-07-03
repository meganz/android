package mega.privacy.android.feature.documentscanner.data.boundary

/**
 * Extracts a tightly-packed grayscale [ByteArray] from the Y (luma) plane of a
 * CameraX `ImageAnalysis` frame.
 *
 * CameraX delivers YUV_420_888 frames where the Y plane may carry padding: each
 * row can be wider than the image ([rowStride] > [width]) and, on some devices,
 * successive luma samples are interleaved ([pixelStride] > 1). The detector
 * expects a dense `width * height` array with no padding, so this converter
 * copies only the meaningful luma samples, dropping row padding and honouring
 * the pixel stride.
 *
 * Pure and framework-free (operates on the plane bytes already copied out of the
 * `ByteBuffer`), so it is unit-testable on the JVM. The fast path — the common
 * `pixelStride == 1` case — copies row by row.
 */
internal class YPlaneToGrayConverter {

    /**
     * @param yPlane The Y-plane bytes, as laid out by the camera (may include
     *   row and/or pixel padding).
     * @param width Image width in pixels.
     * @param height Image height in pixels.
     * @param rowStride Bytes between the start of consecutive rows in [yPlane].
     * @param pixelStride Bytes between consecutive luma samples within a row.
     * @return A dense `width * height` grayscale array.
     */
    fun convert(
        yPlane: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
    ): ByteArray {
        require(width > 0 && height > 0) { "width and height must be positive" }
        require(pixelStride >= 1) { "pixelStride must be >= 1" }
        require(rowStride >= width * pixelStride) { "rowStride too small for width/pixelStride" }
        // Highest index we read: last row start + last sample within the row.
        // Guards devices that trim the final row's trailing padding.
        val maxReadIndex = (height - 1) * rowStride + (width - 1) * pixelStride
        require(yPlane.size > maxReadIndex) {
            "yPlane too small: need > $maxReadIndex bytes but got ${yPlane.size}"
        }

        val out = ByteArray(width * height)
        if (pixelStride == 1) {
            for (row in 0 until height) {
                System.arraycopy(yPlane, row * rowStride, out, row * width, width)
            }
        } else {
            for (row in 0 until height) {
                val rowStart = row * rowStride
                val outStart = row * width
                for (col in 0 until width) {
                    out[outStart + col] = yPlane[rowStart + col * pixelStride]
                }
            }
        }
        return out
    }
}
