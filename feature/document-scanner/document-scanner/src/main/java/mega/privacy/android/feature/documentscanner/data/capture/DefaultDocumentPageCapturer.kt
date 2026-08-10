package mega.privacy.android.feature.documentscanner.data.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.documentscanner.data.boundary.PerspectiveWarper
import mega.privacy.android.feature.documentscanner.domain.capture.DocumentPageCapturer
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.PageQuality
import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage
import java.util.UUID
import javax.inject.Inject

/**
 * Decodes the captured JPEG, makes it upright, rectifies it to the detected
 * boundary via [PerspectiveWarper], then persists the full-resolution image and
 * a thumbnail through [DocumentImageStorage]. All work runs on [ioDispatcher].
 *
 * Quality assessment is not part of this pipeline yet — pages are recorded as
 * [PageQuality.GOOD]; scoring is a later concern.
 */
internal class DefaultDocumentPageCapturer @Inject constructor(
    private val perspectiveWarper: PerspectiveWarper,
    private val imageStorage: DocumentImageStorage,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DocumentPageCapturer {

    override suspend fun capture(
        jpegBytes: ByteArray,
        rotationDegrees: Int,
        boundary: DocumentBoundary?,
    ): ScannedPage = withContext(ioDispatcher) {
        // Track every bitmap we create so they are freed even if warp/save throws —
        // full-res captures are large and a leak here quickly OOMs later captures.
        // The Set dedups the identity cases where a transform returned its input
        // unchanged (0° rotation, null boundary, already-small thumbnail).
        val intermediates = mutableSetOf<Bitmap>()
        try {
            val decoded = (BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                ?: throw IllegalArgumentException("Unable to decode captured frame"))
                .also { intermediates += it }
            val upright = decoded.rotated(rotationDegrees).also { intermediates += it }
            val warped = perspectiveWarper.warp(upright, boundary).also { intermediates += it }
            val thumbnail = warped.scaledToMaxEdge(THUMBNAIL_MAX_EDGE_PX).also { intermediates += it }

            val id = UUID.randomUUID().toString()
            val imageUri = imageStorage.saveJpeg(warped, "$id.jpg")
            val thumbnailUri = imageStorage.saveJpeg(thumbnail, "${id}_thumb.jpg")

            ScannedPage(
                id = id,
                imageUri = imageUri,
                thumbnailUri = thumbnailUri,
                order = 0,
                capturedAt = System.currentTimeMillis(),
                quality = PageQuality.GOOD,
                boundary = boundary,
            )
        } finally {
            intermediates.forEach { it.recycle() }
        }
    }

    private companion object {
        const val THUMBNAIL_MAX_EDGE_PX = 320
    }
}
