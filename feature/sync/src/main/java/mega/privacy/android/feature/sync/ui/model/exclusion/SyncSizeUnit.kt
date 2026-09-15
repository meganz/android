package mega.privacy.android.feature.sync.ui.model.exclusion

/**
 * The unit a size bound is typed in, and the factor converting it to the bytes the file stores.
 *
 * The factors are binary, matching the SDK's own shorthand: it reads `100M` back as 104857600.
 */
internal enum class SyncSizeUnit(val multiplierBytes: Long) {
    Bytes(1L),
    Kilobytes(1_024L),
    Megabytes(1_024L * 1_024L),
    Gigabytes(1_024L * 1_024L * 1_024L),
    ;

    /**
     * [value] converted to bytes, or `null` when it is negative or the product would not fit a
     * `Long`.
     *
     * The overflow is checked by division rather than by inspecting the product, because a `Long`
     * multiplication wraps silently and a wrapped result reads as a plausible, far smaller bound.
     * A negative input is refused for the same reason: it is itself the result of such a wrap.
     */
    fun toBytes(value: Long): Long? = when {
        value < 0 -> null
        value > Long.MAX_VALUE / multiplierBytes -> null
        else -> value * multiplierBytes
    }
}
