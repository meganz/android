package mega.privacy.android.app.components

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat

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

    fun setOnSummaryClickListener(listener: (() -> Unit)?) {
        summaryListener = listener
    }
}
