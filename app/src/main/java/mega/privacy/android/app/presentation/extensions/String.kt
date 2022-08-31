package mega.privacy.android.app.presentation.extensions

import android.content.Context
import android.text.Spanned
import androidx.core.text.HtmlCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.ColorUtils

internal fun String.spanABTextFontColour(
    context: Context,
    aColourHex: String = ColorUtils.getColorHexString(context,
        R.color.grey_900_grey_100),
    bColourHex: String = ColorUtils.getColorHexString(context, R.color.grey_500_grey_400),
    toSpannedString: (String) -> CharSequence = { input ->
        HtmlCompat.fromHtml(input,
            HtmlCompat.FROM_HTML_MODE_LEGACY)
    },
) = runCatching {
    replace("[A]", "<font color='"
            + aColourHex
            + "'>")
        .replace("[/A]", "</font>")
        .replace("[B]", "<font color='"
                + bColourHex
                + "'>")
        .replace("[/B]", "</font>")
}.mapCatching {
    toSpannedString(it)
}.getOrDefault(this)