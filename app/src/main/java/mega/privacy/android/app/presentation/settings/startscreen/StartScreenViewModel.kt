package mega.privacy.android.app.presentation.settings.startscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.appstate.content.mapper.ScreenPreferenceDestinationMapper
import mega.privacy.android.app.presentation.settings.startscreen.mapper.StartScreenDestinationOptionMapper
import mega.privacy.android.app.presentation.settings.startscreen.mapper.StartScreenDestinationPreferenceNavKeyMapper
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenSettingsState
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.usecase.SetStartScreenPreference
import mega.privacy.android.domain.usecase.preference.MonitorStartScreenPreferenceDestinationUseCase
import mega.privacy.android.domain.usecase.preference.SetStartScreenPreferenceDestinationUseCase
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.navigation.contract.qualifier.DefaultStartScreen
import mega.privacy.android.navigation.contract.sortedByPreferredSlot
import mega.privacy.android.core.coroutine.asUiStateFlow
import javax.inject.Inject

@HiltViewModel
class StartScreenViewModel @Inject constructor(
    private val setStartScreenPreference: SetStartScreenPreference,
    private val mainDestinations: Set<@JvmSuppressWildcards MainNavItem>,
    private val monitorStartScreenPreferenceDestinationUseCase: MonitorStartScreenPreferenceDestinationUseCase,
    private val screenPreferenceDestinationMapper: ScreenPreferenceDestinationMapper,
    private val startScreenDestinationPreferenceNavKeyMapper: StartScreenDestinationPreferenceNavKeyMapper,
    private val setStartScreenPreferenceDestinationUseCase: SetStartScreenPreferenceDestinationUseCase,
    startScreenDestinationOptionMapper: StartScreenDestinationOptionMapper,
    @DefaultStartScreen private val defaultStartScreen: MainNavItemNavKey,
) : ViewModel() {

    val state: StateFlow<StartScreenSettingsState> by lazy {
        combine(
            flow { emit(mainDestinations) }
                .map { navItems ->
                    navItems
                        .filter { it.preferredSlot !is PreferredSlot.Last }
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