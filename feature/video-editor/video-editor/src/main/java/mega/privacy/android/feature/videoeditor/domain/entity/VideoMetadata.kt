package mega.privacy.android.feature.videoeditor.domain.entity

/**
 * Metadata read once per loaded source.
 *
 * Beyond the geometry the editor needs to lay out the preview, this also
 * carries the "library" metadata gallery apps (Google Photos, Apple Photos)
 * surface — the original capture date and GPS location — so export can
 * re-attach them to the saved file instead of producing an anonymous clip.
 */
data class VideoMetadata(
    val durationMs: Long,
    val widthPx: Int,
    val heightPx: Int,
    /** Original capture/creation time in Unix epoch ms, or null if unknown. */
    val dateTakenMs: Long? = null,
    /** GPS latitude in degrees, or null if the source has no location tag. */
    val latitude: Float? = null,
    /** GPS longitude in degrees, or null if the source has no location tag. */
    val longitude: Float? = null,
)
