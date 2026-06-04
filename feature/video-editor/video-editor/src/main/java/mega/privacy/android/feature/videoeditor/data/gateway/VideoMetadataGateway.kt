package mega.privacy.android.feature.videoeditor.data.gateway

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.feature.videoeditor.domain.entity.VideoMetadata
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Thin wrapper over the framework [MediaMetadataRetriever]. Synchronous and
 * blocking — the repository switches it onto the IO dispatcher.
 */
class VideoMetadataGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Read duration and post-rotation pixel dimensions of `uriString`, plus the
     * capture date and location when present. Swaps width/height when the
     * metadata reports an odd-multiple rotation (portrait video stored as
     * landscape with a rotation tag, etc.).
     *
     * Returns zeros (and null library metadata) on failure; callers should treat
     * that as "metadata not yet available".
     */
    fun getVideoMetadata(uriString: String): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            // setDataSource throws on an unreadable / revoked / malformed URI; the
            // whole read is wrapped so any such failure resolves to the documented
            // "all zeros" sentinel instead of crashing the loading coroutine.
            retriever.setDataSource(context, Uri.parse(uriString))
            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION,
            )?.toLongOrNull() ?: 0L
            val rotation = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION,
            )?.toIntOrNull() ?: 0
            val rawWidth = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH,
            )?.toIntOrNull() ?: 0
            val rawHeight = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT,
            )?.toIntOrNull() ?: 0
            val (w, h) = if (rotation % 180 == 0) rawWidth to rawHeight else rawHeight to rawWidth

            val dateTakenMs = parseCreationDateMs(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE),
            )
            val location = parseLocationIso6709(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION),
            )
            VideoMetadata(
                durationMs = duration,
                widthPx = w,
                heightPx = h,
                dateTakenMs = dateTakenMs,
                latitude = location?.first,
                longitude = location?.second,
            )
        } catch (_: Throwable) {
            VideoMetadata(durationMs = 0L, widthPx = 0, heightPx = 0)
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Parse a `METADATA_KEY_DATE` string into Unix epoch ms, or null if unparseable. */
    private fun parseCreationDateMs(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        for (pattern in CREATION_DATE_PATTERNS) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    // Patterns ending in the literal 'Z' carry no offset, so the
                    // value is UTC; the numeric-offset variants override this.
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }.parse(raw)
            }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return null
    }


    /**
     * Parse the latitude/longitude out of a `METADATA_KEY_LOCATION` ISO-6709
     * string, ignoring any trailing altitude / CRS suffix. Returns null when the
     * tag is absent or malformed.
     */
    private fun parseLocationIso6709(raw: String?): Pair<Float, Float>? {
        if (raw.isNullOrBlank()) return null
        val match = ISO_6709.find(raw) ?: return null
        val latitude = match.groupValues[1].toFloatOrNull() ?: return null
        val longitude = match.groupValues[2].toFloatOrNull() ?: return null
        return latitude to longitude
    }
}

/**
 * `METADATA_KEY_DATE` candidate formats. Most camera apps emit the first
 * (`yyyyMMdd'T'HHmmss.SSS'Z'`, UTC); the variants tolerate a missing
 * millisecond field or a numeric timezone offset.
 */
private val CREATION_DATE_PATTERNS = listOf(
    "yyyyMMdd'T'HHmmss.SSS'Z'",
    "yyyyMMdd'T'HHmmss'Z'",
    "yyyyMMdd'T'HHmmss.SSSZ",
    "yyyyMMdd'T'HHmmssZ",
)


/** Leading signed lat/long pair of an ISO-6709 string (`+27.5916+086.5640/`). */
private val ISO_6709 = Regex("([+\\-]\\d+(?:\\.\\d+)?)([+\\-]\\d+(?:\\.\\d+)?)")
