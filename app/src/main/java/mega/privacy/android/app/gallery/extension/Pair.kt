package mega.privacy.android.app.gallery.extension

import android.content.Context
import android.text.Spanned
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import timber.log.Timber

/**
 * Format String pair to get date title.
 *
 * @param date String pair, which contains the whole date string.
 * @param context
 * @return Formatted Spanned can be set to TextView.
 */
fun Pair<String?, String?>.formatDateTitle(context: Context): Spanned {
    var dateText =
        if (this.second.isNullOrBlank()) "[B]" + this.first + "[/B]" else context.getFormattedStringOrDefault(
            R.string.cu_month_year_date,
            this.first,
            this.second
        )
    try {
        dateText = dateText.replace("[B]", "<font face=\"sans-serif-medium\">")
            .replace("[/B]", "</font>")
    } catch (e: Exception) {
        Timber.w(e, "Exception formatting text.")
    }

    return dateText.toSpannedHtmlText()
}