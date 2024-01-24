package mega.privacy.android.domain.entity

import kotlin.math.roundToInt

/**
 * Progress
 *
 * @property floatValue from 0f to 1f inclusive
 */
@JvmInline
value class Progress(val floatValue: Float) {

    constructor(current: Number, total: Number) : this(
        if (total == 0) 0f else current.toFloat().div(total.toFloat()).coerceIn(0f, 1f)
    )

    init {
        require(floatValue >= 0) { "Progress cannot be negative" }
        require(floatValue <= 1) { "Progress must be expressed as a range from 0 to 1" }
    }

    /**
     * Returns the progress value as an [Int] with a range of [0, 100]
     */
    val intValue get() = floatValue.times(100f).roundToInt()
}
