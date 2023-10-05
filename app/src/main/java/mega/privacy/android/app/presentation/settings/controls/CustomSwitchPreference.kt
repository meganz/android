package mega.privacy.android.app.presentation.settings.controls

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat

class CustomSwitchPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
) : SwitchPreferenceCompat(context, attrs) {

    override fun onDependencyChanged(dependency: Preference, disableDependent: Boolean) {
        isVisible = !disableDependent
        super.onDependencyChanged(dependency, disableDependent)
    }

}