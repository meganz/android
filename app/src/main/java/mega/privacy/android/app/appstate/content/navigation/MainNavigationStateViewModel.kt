package mega.privacy.android.app.appstate.content.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take
import mega.privacy.android.app.appstate.content.mapper.ScreenPreferenceDestinationMapper
import mega.privacy.android.app.appstate.content.navigation.model.MainNavState
import mega.privacy.android.domain.usecase.featureflag.GetEnabledFlaggedItemsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.preference.MonitorStartScreenPreferenceDestinationUseCase
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.qualifier.DefaultStartScreen
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainNavigationStateViewModel @Inject constructor(
    private val mainDestinations: Set<@JvmSuppressWildcards MainNavItem>,
    private val getEnabledFlaggedItemsUseCase: GetEnabledFlaggedItemsUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorStartScreenPreferenceDestinationUseCase: MonitorStartScreenPreferenceDestinationUseCase,
    private val screenPreferenceDestinationMapper: ScreenPreferenceDestinationMapper,
    @DefaultStartScreen private val defaultStartScreen: NavKey,
) : ViewModel() {

    val state: StateFlow<MainNavState> by lazy {
        combine(
            filteredMainNavItemsFlow()
                .asNavigationItems()
                .log("Navigation Items"),
            filteredMainNavItemsFlow()
                .map { itemSet -> itemSet.map { it.screen }.toSet().toImmutableSet() }
                .log("Main Nav Screens"),
            monitorStartScreenPreferenceDestinationUseCase()
                .map {
                    screenPreferenceDestinationMapper(it) ?: defaultStartScreen
                }.take(1)
                .log("Start Screen Preference Destination"),
        ) { navigationItems, mainScreens, startScreenPreferenceDestination ->
            MainNavState.Data(
                mainNavItems = navigationItems,
                mainNavScreens = mainScreens,
                initialDestination = startScreenPreferenceDestination
            )
        }.catch { Timber.e(it, "Error in NavigationItemStateViewModel") }
            .asUiStateFlow(
                scope = viewModelScope,
                initialValue = MainNavState.Loading
            )
    }

    private fun filteredMainNavItemsFlow(): SharedFlow<Set<@JvmSuppressWildcards MainNavItem>> =
        getEnabledFlaggedItemsUseCase(mainDestinations)
            .shareIn(viewModelScope, started = SharingStarted.WhileSubscribed(200), replay = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<Set<MainNavItem>>.asNavigationItems(): Flow<ImmutableSet<NavigationItem>> {
        return combine(
            getConnectivityStateOrDefault(),
            this.map { mainNavItemsSet ->
                mainNavItemsSet.map { (it.badge ?: flowOf(null)) to it }
            }
        ) { connected: Boolean, badgeFlowPair: List<Pair<Flow<String?>, MainNavItem>> ->
            badgeFlowPair.map { (badgeFlow, mainNavItem) ->
                badgeFlow.map { badgeText ->
                    mapToNavigationItem(
                        mainNavItem = mainNavItem,
                        connected = connected,
                        badgeText = badgeText
                    )
                }
            }
        }.flatMapConcat { flowsList ->
            combine(flowsList) { it.toSet().toImmutableSet() }
        }
    }

    private fun mapToNavigationItem(
        mainNavItem: MainNavItem,
        connected: Boolean,
        badgeText: String?,
    ): NavigationItem = NavigationItem(
        destination = mainNavItem.destination,
        icon = mainNavItem.icon,
        label = mainNavItem.label,
        preferredSlot = mainNavItem.preferredSlot,
        analyticsEventIdentifier = mainNavItem.analyticsEventIdentifier,
        isEnabled = connected || mainNavItem.availableOffline,
        badgeText = badgeText
    )

    private fun getConnectivityStateOrDefault(): Flow<Boolean> = monitorConnectivityUseCase()
        .catch {
            Timber.e(
                it,
                "Error monitoring connectivity, defaulting to connected state"
            )
            emit(true)
        }

    private fun <T> Flow<T>.log(flowName: String): Flow<T> = this.onEach {
        Timber.d("$flowName emitted: $it")
    }
}