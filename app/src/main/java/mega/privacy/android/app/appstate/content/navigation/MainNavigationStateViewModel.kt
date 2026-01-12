package mega.privacy.android.app.appstate.content.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import mega.privacy.android.app.appstate.content.mapper.ScreenPreferenceDestinationMapper
import mega.privacy.android.app.appstate.content.navigation.model.MainNavState
import mega.privacy.android.domain.usecase.featureflag.GetEnabledFlaggedItemsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.preference.MonitorStartScreenPreferenceDestinationUseCase
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.MainNavItemBadge
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.navigation.contract.qualifier.DefaultStartScreen
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainNavigationStateViewModel @Inject constructor(
    mainDestinations: Set<@JvmSuppressWildcards MainNavItem>,
    getEnabledFlaggedItemsUseCase: GetEnabledFlaggedItemsUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorStartScreenPreferenceDestinationUseCase: MonitorStartScreenPreferenceDestinationUseCase,
    private val screenPreferenceDestinationMapper: ScreenPreferenceDestinationMapper,
    @DefaultStartScreen private val defaultStartScreen: MainNavItemNavKey,
    val navigationResultManager: NavigationResultManager,
) : ViewModel() {

    val state: StateFlow<MainNavState> by lazy {
        combine(
            isConnected,
            getNavigationItems()
                .log("Navigation Items"),
            filteredMainNavItemsFlow
                .map { itemSet -> itemSet.map { it.screen }.toSet().toImmutableSet() }
                .log("Main Nav Screens"),
            monitorStartScreenPreferenceDestinationUseCase()
                .map {
                    screenPreferenceDestinationMapper(it) ?: defaultStartScreen
                }.take(1)
                .log("Start Screen Preference Destination"),
        ) { isConnected, navigationItems, mainScreens, startScreenPreferenceDestination ->
            MainNavState.Data(
                mainNavItems = navigationItems,
                mainNavScreens = mainScreens,
                initialDestination = startScreenPreferenceDestination,
                isConnected = isConnected
            )
        }.catch { Timber.e(it, "Error in NavigationItemStateViewModel") }
            .asUiStateFlow(
                scope = viewModelScope,
                initialValue = MainNavState.Loading
            )
    }

    private val isConnected: StateFlow<Boolean> =
        getConnectivityStateOrDefault()
            .stateIn(viewModelScope, SharingStarted.Lazily, true)

    private val filteredMainNavItemsFlow =
        getEnabledFlaggedItemsUseCase(mainDestinations)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getNavigationItems(): Flow<ImmutableSet<NavigationItem>> {
        return filteredMainNavItemsFlow.flatMapConcat { items ->
            val badgeFlowPair: List<Pair<Flow<MainNavItemBadge?>, MainNavItem>> =
                items.map { (it.badge ?: flowOf(null)) to it }
            val navigationItemFlows = badgeFlowPair.map { (badgeFlow, mainNavItem) ->
                combine(
                    isConnected,
                    badgeFlow
                ) { connected: Boolean, badge: MainNavItemBadge? ->
                    mapToNavigationItem(
                        mainNavItem = mainNavItem,
                        connected = connected,
                        badge = badge
                    )
                }
            }
            combine(navigationItemFlows) { it.toSet().toImmutableSet() }
        }
    }

    private fun mapToNavigationItem(
        mainNavItem: MainNavItem,
        connected: Boolean,
        badge: MainNavItemBadge?,
    ): NavigationItem = NavigationItem(
        destination = mainNavItem.destination,
        icon = mainNavItem.icon,
        selectedIcon = mainNavItem.selectedIcon,
        label = mainNavItem.label,
        preferredSlot = mainNavItem.preferredSlot,
        analyticsEventIdentifier = mainNavItem.analyticsEventIdentifier,
        isEnabled = connected || mainNavItem.availableOffline,
        badge = badge
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