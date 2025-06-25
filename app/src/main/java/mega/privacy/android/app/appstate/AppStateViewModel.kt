package mega.privacy.android.app.appstate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.app.appstate.model.AppState
import mega.privacy.android.app.appstate.model.AppStateDataBuilder
import mega.privacy.android.domain.entity.navigation.Flagged
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.navigation.GetStartScreenPreferenceDestinationUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val mainDestinations: Set<@JvmSuppressWildcards MainNavItem>,
    private val featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getStartScreenPreferenceDestinationUseCase: GetStartScreenPreferenceDestinationUseCase, // We need a new use case
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) : ViewModel() {

    val state: StateFlow<AppState> by lazy {
        combine(
            getFilteredValues(mainDestinations)
                .setEnabledState()
                .log("Main Destinations"),
            getFilteredValues(featureDestinations)
                .log("Feature Destinations"),
//            getStartScreenPreferenceDestinationUseCase()
            flow { emit(mainDestinations.first().destination) }
                .log("Start Screen Preference Destination"),
            monitorThemeModeUseCase()
                .log("Theme Mode")
        ) { mainItems, featureItems, startDestination, themeMode ->
            AppStateDataBuilder()
                .mainNavItems(mainItems)
                .featureDestinations(featureItems)
                .initialDestination(startDestination)
                .themeMode(themeMode)
                .build()
        }.catch {
            Timber.e(it, "Error while building app state")
        }.onEach {
            Timber.d("AppState emitted: $it")
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(200),
            initialValue = AppState.Loading
        )
    }

    private fun <T> getFilteredValues(items: Set<T>): Flow<Set<T>> = flow {
        val filteredItems = items.filter { item ->
            if (item is Flagged) {
                getFeatureFlagValueUseCase(item.feature)
            } else {
                true
            }
        }.toSet()
        emit(filteredItems)
        awaitCancellation()
    }

    private fun <T> Flow<T>.log(flowName: String): Flow<T> = this.onEach {
        Timber.d("$flowName emitted: $it")
    }

    private fun Flow<Set<MainNavItem>>.setEnabledState(): Flow<Set<NavigationItem>> =
        this.combine(
            monitorConnectivityUseCase()
                .catch {
                    Timber.e(
                        it,
                        "Error monitoring connectivity, defaulting to connected state"
                    )
                    emit(true)
                }
        ) { mainNavItems, isConnected ->
            mainNavItems.map { item ->
                NavigationItem(
                    navItem = item,
                    isEnabled = isConnected || item.availableOffline,
                )
            }.toSet()
        }
}
