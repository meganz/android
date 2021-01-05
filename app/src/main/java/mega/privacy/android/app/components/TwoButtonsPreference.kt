package mega.privacy.android.app.components

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import mega.privacy.android.app.R

class TwoButtonsPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    private var button1Text: String? = null
    private var button2Text: String? = null
    private var button1Listener: (() -> Unit)? = null
    private var button2Listener: (() -> Unit)? = null

    init {
        layoutResource = R.layout.preference_two_buttons
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        (holder.findViewById(R.id.btn_first) as Button?)?.apply {
            text = button1Text
            setOnClickListener { button1Listener?.invoke() }
        }

        (holder.findViewById(R.id.btn_second) as Button?)?.apply {
            text = button2Text
            setOnClickListener { button2Listener?.invoke() }
        }
    }

    fun setButton1(text: String?, listener: (() -> Unit)?) {
        button1Text = text
        button1Listener = listener
    }

    fun setButton2(text: String?, listener: (() -> Unit)?) {
        button2Text = text
        button2Listener = listener
    }
}
