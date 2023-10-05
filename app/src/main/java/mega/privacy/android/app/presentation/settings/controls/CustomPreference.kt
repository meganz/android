package mega.privacy.android.app.presentation.settings.controls

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference

class CustomPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
) : Preference(context, attrs) {

    override fun onDependencyChanged(dependency: Preference, disableDependent: Boolean) {
        isVisible = !disableDependent
        super.onDependencyChanged(dependency, disableDependent)
    }

}