package mega.privacy.android.app.components

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import mega.privacy.android.app.R

/**
 * A Preference that provides two configurable buttons.
 *
 * @param context      The Context this is associated with, through which it can access the
 *                     current theme, resources, {@link SharedPreferences}, etc.
 * @param attrs        The attributes of the XML tag that is inflating the preference
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style
 *                     resource that supplies default values for the view. Can be 0 to not
 *                     look for defaults.
 */
class TwoButtonsPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = android.R.attr.preferenceStyle
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

    /**
     * Set first button configuration
     *
     * @param text      Text to be shown
     * @param listener  Listener to be called when the button is clicked
     */
    fun setButton1(text: String?, listener: (() -> Unit)?) {
        button1Text = text
        button1Listener = listener
    }

    /**
     * Set second button configuration
     *
     * @param text      Text to be shown
     * @param listener  Listener to be called when the button is clicked
     */
    fun setButton2(text: String?, listener: (() -> Unit)?) {
        button2Text = text
        button2Listener = listener
    }
}
