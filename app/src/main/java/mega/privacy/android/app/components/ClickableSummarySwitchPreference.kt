package mega.privacy.android.app.components

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.text.bold
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat


/**
 * A SwitchPreference that provides a click listener for the end part of the summary.
 *
 * @param context      The {@link Context} that will style this preference
 * @param attrs        Style attributes that differ from the default
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style
 *                     resource that supplies default values for the view. Can be 0 to not
 *                     look for defaults.
 * @param defStyleRes  A resource identifier of a style resource that supplies default values
 *                     for the view, used only if defStyleAttr is 0 or can not be found in the
 *                     theme. Can be 0 to not look for defaults.
 */
class ClickableSummarySwitchPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = androidx.preference.R.attr.switchPreferenceCompatStyle,
    defStyleRes: Int = 0
) : SwitchPreferenceCompat(context, attrs, defStyleAttr, defStyleRes) {

    private var summaryText: String? = null
    private var clickableText: String? = null
    private var clickListener: (() -> Unit)? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        if (clickListener != null && !summaryText.isNullOrBlank() && !clickableText.isNullOrBlank()) {
            (holder.findViewById(android.R.id.summary) as TextView?)?.apply {
                val stringBuilder = SpannableStringBuilder()
                    .append(summaryText)
                    .append(" ")
                    .bold { append(clickableText) }

                stringBuilder.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        holder.itemView.performClick()
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.isUnderlineText = false
                    }
                }, 0, summaryText!!.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)


                stringBuilder.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            clickListener?.invoke()
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.isUnderlineText = false
                        }
                    },
                    summaryText!!.length + 1,
                    summaryText!!.length + 1 + clickableText!!.length,
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE
                )

                text = stringBuilder
                linksClickable = true
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    /**
     * Set summary text to be shown.
     *
     * @param text  Text to be shown
     */
    fun setSummaryText(text: String) {
        summaryText = text
    }

    /**
     * Set clickable text to be placed after the summary text.
     *
     * @param text      Clickable text to be shown
     * @param listener  Click listener
     */
    fun setClickableText(text: String, listener: (() -> Unit)?) {
        clickableText = text
        clickListener = listener
    }
}
