package mega.privacy.android.app.presentation.settings.container

import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.app.presentation.settings.container.model.SettingContainerState
import mega.privacy.android.navigation.settings.FeatureSettings
import javax.inject.Inject

@HiltViewModel
class SettingContainerViewModel @Inject constructor(
    private val featureSettings: Set<@JvmSuppressWildcards FeatureSettings>
) : ViewModel() {
    private val _state = MutableStateFlow(
        SettingContainerState(
            nestedGraphs = featureSettings.map { it.settingsNavGraph }
        )
    )
    val state: StateFlow<SettingContainerState> = _state.asStateFlow()

    fun getScreenTitle(navBackStackEntry: NavBackStackEntry?) = navBackStackEntry?.let { entry ->
        featureSettings.firstNotNullOfOrNull { settings: FeatureSettings ->
            settings.getTitleForDestination(
                entry
            )
        }
    }

}
