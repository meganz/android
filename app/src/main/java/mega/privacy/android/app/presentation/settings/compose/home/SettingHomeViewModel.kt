package mega.privacy.android.app.presentation.settings.compose.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingsHomeState
import mega.privacy.android.navigation.settings.FeatureSettingEntryPoint
import mega.privacy.android.navigation.settings.MoreSettingEntryPoint
import javax.inject.Inject

/**
 * Setting container view model
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class SettingHomeViewModel @Inject constructor(
    featureEntryPoints: Set<@JvmSuppressWildcards FeatureSettingEntryPoint>,
    moreEntryPoints: Set<@JvmSuppressWildcards MoreSettingEntryPoint>,
) : ViewModel() {

    val state: StateFlow<SettingsHomeState>
        field: MutableStateFlow<SettingsHomeState> = MutableStateFlow(
            SettingsHomeState.Loading(
                featureEntryPoints = featureEntryPoints
                    .sortedBy { it.preferredOrdinal }
                    .toImmutableList(),
                moreEntryPoints = moreEntryPoints
                    .sortedBy { it.preferredOrdinal }
                    .toImmutableList(),
            )
        )
}
