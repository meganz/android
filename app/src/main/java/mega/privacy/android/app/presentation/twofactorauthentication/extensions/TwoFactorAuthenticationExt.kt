package mega.privacy.android.app.presentation.twofactorauthentication.extensions

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.core.text.isDigitsOnly

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

internal fun String.isValid2FA() = length == NUMBER_PINS && isDigitsOnly()

val DrawableResId = SemanticsPropertyKey<Int>("DrawableResId")
var SemanticsPropertyReceiver.drawableId by DrawableResId
internal const val NUMBER_PINS = 6
