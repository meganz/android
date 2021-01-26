package mega.privacy.android.app.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import mega.privacy.android.app.R

/**
 * A SwitchPreference that cannot be clicked and has a static switch color.
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
class StaticSwitchPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = androidx.preference.R.attr.switchPreferenceCompatStyle,
    defStyleRes: Int = 0
) : SwitchPreferenceCompat(context, attrs, defStyleAttr, defStyleRes) {

    init {
        isEnabled = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        (holder.findViewById(R.id.switchWidget) as SwitchCompat?)?.apply {
            val color = ResourcesCompat.getColor(resources, R.color.disable_fab_invite_contact, null)
            thumbTintList = ColorStateList.valueOf(color)
        }
    }
}
