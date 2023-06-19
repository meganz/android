package mega.privacy.android.app.presentation.twofactorauthentication.extensions

import androidx.core.text.isDigitsOnly
import mega.privacy.android.app.presentation.twofactorauthentication.view.FIFTH_PIN
import mega.privacy.android.app.presentation.twofactorauthentication.view.FIRST_PIN
import mega.privacy.android.app.presentation.twofactorauthentication.view.FOURTH_PIN
import mega.privacy.android.app.presentation.twofactorauthentication.view.NUMBER_PINS
import mega.privacy.android.app.presentation.twofactorauthentication.view.SECOND_PIN
import mega.privacy.android.app.presentation.twofactorauthentication.view.SIXTH_PIN
import mega.privacy.android.app.presentation.twofactorauthentication.view.THIRD_PIN

internal fun String.toSeedArray(): ArrayList<String> {
    val seedLength = 13
    var index = 0
    val seedArray = ArrayList<String>()
    for (i in 0 until seedLength) {
        seedArray.add(this.substring(index, index + 4))
        index += 4
    }
    return seedArray

}

/**
 * Gets the two factor authentication as String format if the List<String> is correct.
 * The List<String> is correct if it contains 6 chars and all of them are digits.
 *
 * @return Two factor authentication as String format.
 */
internal fun List<String>.getTwoFactorAuthentication(): String? =
    joinToString(separator = "").takeIf { it.isValid2FA() }

/**
 * Gets the two factor authentication as List<String> format if the String is correct.
 * The string is correct if it contains 6 chars and all of them are digits.
 *
 * @return Two factor authentication as List<String> format.
 */
internal fun String.getTwoFactorAuthentication(): List<String>? =
    if (isValid2FA()) {
        split("").filter { it.isNotEmpty() }
    } else {
        null
    }

internal fun String.isValid2FA() = length == NUMBER_PINS && isDigitsOnly()

/**
 * Updates the current two factor authentication with the modified pin.
 *
 * @param pin Modified pin.
 * @param index Index to update with the modified pin.
 * @return Two factor authentication as List<String> format
 */
internal fun List<String>.getUpdatedTwoFactorAuthentication(
    pin: String,
    index: Int,
): List<String> =
    when (index) {
        FIRST_PIN -> listOf(
            pin,
            this[SECOND_PIN],
            this[THIRD_PIN],
            this[FOURTH_PIN],
            this[FIFTH_PIN],
            this[SIXTH_PIN]
        )

        SECOND_PIN -> listOf(
            this[FIRST_PIN],
            pin,
            this[THIRD_PIN],
            this[FOURTH_PIN],
            this[FIFTH_PIN],
            this[SIXTH_PIN]
        )

        THIRD_PIN -> listOf(
            this[FIRST_PIN],
            this[SECOND_PIN],
            pin,
            this[FOURTH_PIN],
            this[FIFTH_PIN],
            this[SIXTH_PIN]
        )

        FOURTH_PIN -> listOf(
            this[FIRST_PIN],
            this[SECOND_PIN],
            this[THIRD_PIN],
            pin,
            this[FIFTH_PIN],
            this[SIXTH_PIN]
        )

        FIFTH_PIN -> listOf(
            this[FIRST_PIN],
            this[SECOND_PIN],
            this[THIRD_PIN],
            this[FOURTH_PIN],
            pin,
            this[SIXTH_PIN]
        )

        else -> listOf(
            this[FIRST_PIN],
            this[SECOND_PIN],
            this[THIRD_PIN],
            this[FOURTH_PIN],
            this[FIFTH_PIN],
            pin
        )
    }