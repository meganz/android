package mega.privacy.android.feature.documentscanner.presentation.model

import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary

/**
 * What the overlay needs to draw the detected document quad on top of the
 * camera preview.
 *
 * Corners live in [boundary] as normalised [0,1] coordinates relative to the
 * analysed (post-rotation) frame. Rendering to view pixels needs the frame's
 * aspect ratio, so [frameWidth] / [frameHeight] travel alongside — the overlay
 * maps normalised → view space with a FILL_CENTER (centre-crop) transform to
 * match the `PreviewView` scale type. See `BoundaryOverlayMapper`.
 *
 * A null [boundary] means "no document detected in the latest frame" — the
 * overlay draws nothing (only the static guide, if any).
 *
 * @property boundary The smoothed, normalised document boundary, or null when
 *   nothing was detected in the latest analysed frame.
 * @property frameWidth Width of the analysed (post-rotation) frame in pixels.
 * @property frameHeight Height of the analysed (post-rotation) frame in pixels.
 */
internal data class BoundaryOverlayState(
    val boundary: DocumentBoundary? = null,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
)
