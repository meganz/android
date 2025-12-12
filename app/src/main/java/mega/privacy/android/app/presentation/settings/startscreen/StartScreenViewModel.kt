package mega.privacy.android.app.presentation.settings.startscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.appstate.content.mapper.ScreenPreferenceDestinationMapper
import mega.privacy.android.app.presentation.settings.startscreen.mapper.StartScreenDestinationOptionMapper
import mega.privacy.android.app.presentation.settings.startscreen.mapper.StartScreenDestinationPreferenceNavKeyMapper
import mega.privacy.android.app.presentation.settings.startscreen.mapper.StartScreenOptionMapper
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenSettingsState
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.SetStartScreenPreference
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.preference.MonitorStartScreenPreferenceDestinationUseCase
import mega.privacy.android.domain.usecase.preference.SetStartScreenPreferenceDestinationUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.navigation.contract.qualifier.DefaultStartScreen
import mega.privacy.android.navigation.contract.sortedByPreferredSlot
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import javax.inject.Inject

@HiltViewModel
class StartScreenViewModel @Inject constructor(
    private val monitorStartScreenPreference: MonitorStartScreenPreference,
    private val setStartScreenPreference: SetStartScreenPreference,
    startScreenOptionMapper: StartScreenOptionMapper,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val mainDestinations: Set<@JvmSuppressWildcards MainNavItem>,
    private val monitorStartScreenPreferenceDestinationUseCase: MonitorStartScreenPreferenceDestinationUseCase,
    private val screenPreferenceDestinationMapper: ScreenPreferenceDestinationMapper,
    private val startScreenDestinationPreferenceNavKeyMapper: StartScreenDestinationPreferenceNavKeyMapper,
    private val setStartScreenPreferenceDestinationUseCase: SetStartScreenPreferenceDestinationUseCase,
    startScreenDestinationOptionMapper: StartScreenDestinationOptionMapper,
    @DefaultStartScreen private val defaultStartScreen: MainNavItemNavKey,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<StartScreenSettingsState> by lazy {
        flow { emit(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) }
            .flatMapLatest { singleActivityFlagEnabled ->
                if (singleActivityFlagEnabled) {
                    combine(
                        flow { emit(mainDestinations) }
                            .map { navItems ->
                                navItems
                                    .sortedByPreferredSlot()
                                    .map { startScreenDestinationOptionMapper(it) }
                            },
                        monitorStartScreenPreferenceDestinationUseCase()
                            .map { screenPreferenceDestinationMapper(it) }
                    ) { options, selectedScreen ->
                        StartScreenSettingsState.Data(
                            options = options,
                            selectedScreen = selectedScreen ?: defaultStartScreen,
                        )
                    }
                } else {
                    monitorStartScreenPreference()
                        .map { screen ->
                            StartScreenSettingsState.LegacyData(
                                options = StartScreen.entries
                                    .mapNotNull(startScreenOptionMapper),
                                selectedScreen = screen,
                            )
                        }
                }
            }.asUiStateFlow(viewModelScope, StartScreenSettingsState.Loading)
    }


    fun newScreenClicked(newScreen: StartScreen) {
        viewModelScope.launch {
            setStartScreenPreference(newScreen)
        }
    }

    fun navDestinationClicked(navKey: NavKey) {
        viewModelScope.launch {
            val destination = startScreenDestinationPreferenceNavKeyMapper(navKey)
            if (destination != null) {
                setStartScreenPreferenceDestinationUseCase(destination)
            }
        }
    }
}