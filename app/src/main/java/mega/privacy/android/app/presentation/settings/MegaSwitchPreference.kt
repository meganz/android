package mega.privacy.android.app.presentation.settings

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch


class MegaSwitchPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = android.R.attr.preferenceStyle,
) : SwitchPreferenceCompat(context, attrs, defStyleAttr) {

    init {
        widgetLayoutResource =
            R.layout.preference_switch // SwitchPreferenceCompat will use R.id.switchWidget to find the widget, so it's important to keep it
    }

    @SuppressLint("RestrictedApi")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        val customSwitch = holder.findViewById(R.id.switchWidget) as MegaSwitch
        customSwitch.setOnCheckedChangeListener(null)
        super.onBindViewHolder(holder)
        customSwitch.setOnCheckedChangeListener { _, isChecked ->
            performClick()
        }
    }
}