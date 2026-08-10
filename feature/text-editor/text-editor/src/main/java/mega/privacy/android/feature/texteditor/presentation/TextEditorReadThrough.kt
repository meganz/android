package mega.privacy.android.feature.texteditor.presentation

/**
 * Estimates how far the bottom of the viewport has reached through the document, as a
 * fraction (0.0–1.0) of the file's total line count.
 *
 * A chunk renders its lines as a single LazyColumn item, so the line at the bottom of the
 * viewport is derived from the last visible chunk's pixel geometry. The chunk's line span is
 * taken from the next chunk's start line rather than assumed, because View mode chunks by
 * character capacity (long-line chunking), so a chunk does not hold a fixed number of lines.
 * This stays accurate even when the whole file is a single chunk (the common case for short
 * files), where chunk-index based progress cannot distinguish positions.
 *
 * @param chunkStartLine the 1-indexed line number where the last visible chunk starts.
 * @param nextChunkStartLine the 1-indexed start line of the following chunk, or
 *   `totalLines + 1` when the last visible chunk is the final one.
 * @param chunkSizePx the pixel height of the last visible chunk item.
 * @param chunkOffsetPx the last visible chunk's top offset relative to the viewport start
 *   (negative once it has been scrolled above the top of the viewport).
 * @param viewportEndOffsetPx the bottom edge of the viewport, in pixels.
 * @param totalLines the total number of lines in the document.
 * @return the read-through fraction, coerced to 0.0–1.0; 0f when it cannot be determined
 *   (no content, or the chunk has not been measured yet).
 */
internal fun computeReadThroughFraction(
    chunkStartLine: Int,
    nextChunkStartLine: Int,
    chunkSizePx: Int,
    chunkOffsetPx: Int,
    viewportEndOffsetPx: Int,
    totalLines: Int,
): Float {
    if (totalLines <= 0) return 0f
    val linesInChunk = (nextChunkStartLine - chunkStartLine).coerceAtLeast(1)
    val lineHeightPx = chunkSizePx.toFloat() / linesInChunk
    // Not yet measured: treat as not read through rather than risk a spurious exclusion.
    if (lineHeightPx <= 0f) return 0f
    val linesAboveViewportBottom =
        ((viewportEndOffsetPx - chunkOffsetPx) / lineHeightPx).toInt().coerceIn(0, linesInChunk)
    val lastVisibleLine = chunkStartLine + linesAboveViewportBottom - 1
    return (lastVisibleLine.toFloat() / totalLines.toFloat()).coerceIn(0f, 1f)
}
