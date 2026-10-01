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
        if (total.toFloat() == 0f) 0f else current.toFloat().div(total.toFloat()).coerceIn(0f, 1f)
    )

    init {
        require(floatValue >= 0) { "Progress must be 0 or positive: $floatValue" }
        require(floatValue <= 1) { "Progress must be expressed as a range from 0 to 1" }
    }

    /**
     * Returns the progress value as an [Int] with a range of [0, 100]
     */
    val intValue get() = floatValue.times(100f).roundToInt()
}

/**
 * Extension function to multiply a [Long] by a [Progress]
 */
operator fun Long.times(progress: Progress) =
    (this * progress.floatValue.toDouble()).toLong()

/**
 * Extension function to multiply a [Progress] by a [Long]
 */
operator fun Progress.times(long: Long) =
    (long * this.floatValue.toDouble()).toLong()

/**
 * Extension function to multiply a [Double] by a [Progress]
 */
operator fun Double.times(progress: Progress) = this * progress.floatValue.toDouble()

/**
 * Extension function to multiply a [Progress] by a [Double]
 */
operator fun Progress.times(double: Double) = double * this.floatValue.toDouble()
