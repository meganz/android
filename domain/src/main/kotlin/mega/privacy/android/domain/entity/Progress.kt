package mega.privacy.android.domain.entity

/**
 * Progress
 *
 * @property floatValue from 0f to 1f inclusive
 */
@JvmInline
value class Progress(val floatValue: Float) {
    init {
        require(floatValue >= 0) { "Progress cannot be negative" }
        require(floatValue <= 1) { "Progress must be expressed as a range from 0 to 1" }
    }
}
