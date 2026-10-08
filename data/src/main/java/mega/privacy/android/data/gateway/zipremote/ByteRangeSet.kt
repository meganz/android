package mega.privacy.android.data.gateway.zipremote

import java.util.TreeMap

/**
 * The parts of a remote archive already copied into its local sparse file, kept merged so a later
 * read asks the server only for what is still missing.
 *
 * Not thread-safe: the archive it belongs to serialises access under its own lock.
 */
internal class ByteRangeSet {

    private val ranges = TreeMap<Long, Long>()

    val filledBytes: Long
        get() = ranges.entries.sumOf { it.value - it.key }

    /**
     * Records that `[start, endExclusive)` is now local, merging it with the ranges it touches.
     */
    fun add(start: Long, endExclusive: Long) {
        if (endExclusive <= start) return
        var newStart = start
        var newEnd = endExclusive
        ranges.floorEntry(start)?.takeIf { it.value >= start }?.let {
            newStart = it.key
            newEnd = maxOf(newEnd, it.value)
        }
        val overlapping = ranges.subMap(newStart, true, newEnd, true)
        overlapping.values.maxOrNull()?.let { newEnd = maxOf(newEnd, it) }
        overlapping.clear()
        ranges[newStart] = newEnd
    }

    /**
     * The parts of `[start, endExclusive)` that are not local yet, in ascending order.
     */
    fun missing(start: Long, endExclusive: Long): List<LongRange> {
        val gaps = mutableListOf<LongRange>()
        var cursor = start
        val first = ranges.floorKey(start) ?: start
        for ((from, to) in ranges.tailMap(first, true)) {
            if (from >= endExclusive) break
            if (to <= cursor) continue
            if (from > cursor) gaps += cursor until from
            cursor = maxOf(cursor, to)
        }
        if (cursor < endExclusive) gaps += cursor until endExclusive
        return gaps
    }
}
