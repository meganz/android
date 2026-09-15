package mega.privacy.android.core.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp

/**
 * Encodes [text] into a QR code bitmap off the main thread and keeps it across recompositions,
 * re-encoding only when [text] or the resolved pixel size changes.
 *
 * The bitmap is monochrome; apply the colour where it is rendered so that a theme change repaints
 * the existing bitmap instead of encoding a new one.
 *
 * Under Compose inspection — previews and screenshot tests — the code is encoded during composition
 * instead. Those renderers capture the first composition without awaiting background work, so the
 * asynchronous path would always be caught at its null initial value and record an empty QR code.
 *
 * @param text the value to encode, or null when there is nothing to show.
 * @param size the edge length the code is encoded for.
 * @return the encoded bitmap, or null while encoding is in flight or when [text] cannot be encoded.
 */
@Composable
fun rememberQrCodeBitmap(
    text: String?,
    size: Dp,
): ImageBitmap? {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    return if (LocalInspectionMode.current) {
        remember(text, sizePx) {
            text?.takeIf { it.isNotBlank() }
                ?.let { encodeQrCodeBitmap(text = it, width = sizePx, height = sizePx) }
                ?.asImageBitmap()
        }
    } else {
        produceState<ImageBitmap?>(initialValue = null, text, sizePx) {
            value = text?.takeIf { it.isNotBlank() }
                ?.let { encodeQrCode(text = it, width = sizePx, height = sizePx)?.asImageBitmap() }
        }.value
    }
}
