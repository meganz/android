package mega.privacy.android.app.domain.entity

import androidx.annotation.FloatRange

/**
 * A value class representing the progress of an action as a float from 0 to 1 inclusive
 */
@JvmInline
value class Progress(@FloatRange(from = 0.0, to = 1.0) val floatValue: Float) {
    init {
        require(floatValue >= 0) { "Progress cannot be negative" }
        require(floatValue <= 1) { "Progress must be expressed as a range from 0 to 1" }
    }
}
