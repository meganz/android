package mega.privacy.android.feature.documentscanner.data.boundary

import javax.inject.Inject
import java.util.Arrays

/**
 * Finds the largest 4-connected foreground component (`mask > threshold`)
 * in a 2D float mask, and returns its four extreme-corner pixels.
 *
 * "Extreme corners" are the pixels minimising / maximising `x + y` (TL / BR)
 * and `x - y` (BL / TR) over the component. For a roughly rectangular mask
 * these land on the actual document corners and require no further
 * post-processing.
 *
 * Single-pass flood fill with online extreme tracking, no allocations on the
 * hot path: scratch buffers are reused across calls and resized lazily when
 * the input dimensions change.
 *
 * **Not thread-safe.** It holds mutable scratch state ([visited], [floodStack],
 * [stackTop]) across a call, so a single instance must not run concurrent
 * [findLargest] calls. It is intentionally **not** a `@Singleton`: each
 * consumer gets its own instance (the boundary detector constructor-injects
 * one and drives it from a single analysis thread), which keeps the buffer
 * reuse without sharing mutable state app-wide.
 */
class LargestComponentFinder @Inject constructor() {

    /**
     * Plain class (not a `data class`) on purpose: with [IntArray] fields the
     * generated `equals`/`hashCode`/`toString` would compare/print the arrays
     * by reference, which is misleading. This is a one-shot return holder read
     * by property, so it needs no structural equality.
     */
    class ExtremeCorners(
        val tl: IntArray,
        val tr: IntArray,
        val br: IntArray,
        val bl: IntArray,
        val componentSize: Int,
    )

    private var visited: BooleanArray = BooleanArray(0)
    private var floodStack: IntArray = IntArray(0)
    private var stackTop: Int = 0

    /**
     * @param mask Row-major foreground mask of shape [height]×[width].
     * @param width Mask width in pixels.
     * @param height Mask height in pixels.
     * @param threshold Pixels with `mask[i] > threshold` are foreground.
     * @param minComponentPixels Reject candidates smaller than this. Use to
     *   filter speckle / single-cell noise.
     * @return Extreme corners + component size of the largest component, or
     *   null when no component meets [minComponentPixels].
     */
    fun findLargest(
        mask: FloatArray,
        width: Int,
        height: Int,
        threshold: Float,
        minComponentPixels: Int,
    ): ExtremeCorners? {
        val total = width * height
        if (mask.size < total) return null
        ensureScratchSized(total)
        Arrays.fill(visited, 0, total, false)

        var bestSize = 0
        var bestTlIdx = 0
        var bestTrIdx = 0
        var bestBrIdx = 0
        var bestBlIdx = 0

        for (start in 0 until total) {
            if (visited[start] || mask[start] <= threshold) continue

            stackTop = 0
            push(start)
            visited[start] = true

            var componentSize = 0
            var tlSum = Int.MAX_VALUE; var tlIdx = start
            var brSum = Int.MIN_VALUE; var brIdx = start
            var trDiff = Int.MIN_VALUE; var trIdx = start
            var blDiff = Int.MAX_VALUE; var blIdx = start

            while (stackTop > 0) {
                val idx = floodStack[--stackTop]
                componentSize++

                val x = idx % width
                val y = idx / width
                val sum = x + y
                val diff = x - y
                if (sum < tlSum) { tlSum = sum; tlIdx = idx }
                if (sum > brSum) { brSum = sum; brIdx = idx }
                if (diff > trDiff) { trDiff = diff; trIdx = idx }
                if (diff < blDiff) { blDiff = diff; blIdx = idx }

                if (x > 0) visitNeighbour(idx - 1, mask, threshold)
                if (x < width - 1) visitNeighbour(idx + 1, mask, threshold)
                if (y > 0) visitNeighbour(idx - width, mask, threshold)
                if (y < height - 1) visitNeighbour(idx + width, mask, threshold)
            }

            if (componentSize > bestSize) {
                bestSize = componentSize
                bestTlIdx = tlIdx
                bestTrIdx = trIdx
                bestBrIdx = brIdx
                bestBlIdx = blIdx
            }
        }

        if (bestSize < minComponentPixels) return null

        return ExtremeCorners(
            tl = toXY(bestTlIdx, width),
            tr = toXY(bestTrIdx, width),
            br = toXY(bestBrIdx, width),
            bl = toXY(bestBlIdx, width),
            componentSize = bestSize,
        )
    }

    private fun visitNeighbour(neighbour: Int, mask: FloatArray, threshold: Float) {
        if (visited[neighbour]) return
        visited[neighbour] = true
        if (mask[neighbour] > threshold) push(neighbour)
    }

    // A cell is marked visited *before* it is ever pushed (here and at the
    // seed in findLargest), so each of the `width * height` cells is pushed at
    // most once across a fill. floodStack is sized to width * height, so
    // stackTop can never exceed its capacity — no overflow / AIOOBE.
    private fun push(value: Int) {
        floodStack[stackTop++] = value
    }

    private fun ensureScratchSized(size: Int) {
        if (visited.size < size) visited = BooleanArray(size)
        if (floodStack.size < size) floodStack = IntArray(size)
    }

    private fun toXY(idx: Int, width: Int): IntArray = intArrayOf(idx % width, idx / width)
}
