package mega.privacy.android.app.presentation.settings.compose.container

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.navigation.settings.FeatureSettings
import javax.inject.Inject

@HiltViewModel
class SettingContainerViewModel @Inject constructor(
    featureSettings: Set<@JvmSuppressWildcards FeatureSettings>
) : ViewModel() {
    val state: StateFlow<SettingContainerState>
        field = MutableStateFlow(
            SettingContainerState(
                nestedGraphs = featureSettings.map { it.settingsNavGraph }
            )
        )
}
