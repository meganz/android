package mega.privacy.android.app.components

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat

/**
 * A SwitchPreference that provides a click listener for the summary.
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

    private var summaryListener: (() -> Unit)? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        if (summaryListener != null) {
            (holder.findViewById(android.R.id.summary) as TextView?)?.apply {
                setOnClickListener { summaryListener?.invoke() }
            }
        }
    }

    /**
     * Set summary click listener to be called when the summary text is clicked.
     *
     * @param listener Callback to be called
     */
    fun setOnSummaryClickListener(listener: (() -> Unit)?) {
        summaryListener = listener
    }
}
