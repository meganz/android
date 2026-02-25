package mega.privacy.android.domain.entity

/**
 * Currency value class
 *
 * @property code
 */
@JvmInline
value class Currency(val code: String) {
    val isValid: Boolean
        get() = code.length == 3 && code.all { it.isLetter() }
}
