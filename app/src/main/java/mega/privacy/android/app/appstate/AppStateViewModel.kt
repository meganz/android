package mega.privacy.android.app.appstate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.appstate.model.AppState
import mega.privacy.android.app.appstate.model.AppStateDataBuilder
import mega.privacy.android.domain.entity.navigation.Flagged
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.chat.RetryConnectionsAndSignalPresenceUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.shared.original.core.ui.utils.asUiStateFlow
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val mainDestinations: Set<@JvmSuppressWildcards MainNavItem>,
    private val featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val retryConnectionsAndSignalPresenceUseCase: RetryConnectionsAndSignalPresenceUseCase,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
) : ViewModel() {

    private val updatePresenceFlow = MutableStateFlow(false)

    private val getStartScreenPreferenceDestinationUseCase: suspend () -> Any =
        { mainDestinations.first().destination } // Inject as param once we have a new use case for it

    val state: StateFlow<AppState> by lazy {
        combine(
            filteredMainNavItemsFlow()
                .asNavigationItems()
                .log("NavigationItems"),
            filteredMainNavItemsFlow()
                .map { itemSet -> itemSet.map { it.screen }.toSet() }
                .log("Main Nav Screens"),
            getFilteredValues(featureDestinations)
                .log("Feature Destinations"),
            flow { emit(getStartScreenPreferenceDestinationUseCase()) }
                .log("Start Screen Preference Destination"),
            monitorFetchNodesFinishUseCase()
                .onStart { emit(rootNodeExistsUseCase()) }
                .catch { Timber.e(it, "Error monitoring fetch nodes finish") }
                .log("Fetch Nodes Finish"),
        ) { mainItems, mainScreens, featureItems, startDestination, rootNodeExist ->
            if (rootNodeExist) {
                AppStateDataBuilder()
                    .mainNavItems(mainItems)
                    .mainNavScreens(mainScreens)
                    .featureDestinations(featureItems)
                    .initialDestination(startDestination)
                    .build()
            } else {
                AppState.FetchingNodes
            }
        }.onStart {
            trackPresence()
        }.catch {
            Timber.e(it, "Error while building app state")
        }.onEach {
            Timber.d("AppState emitted: $it")
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = AppState.Loading
        )
    }

    private fun filteredMainNavItemsFlow(): SharedFlow<Set<@JvmSuppressWildcards MainNavItem>> =
        getFilteredValues(mainDestinations)
            .shareIn(viewModelScope, started = SharingStarted.WhileSubscribed(200), replay = 1)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<Set<MainNavItem>>.asNavigationItems(): Flow<Set<NavigationItem>> {
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
            combine(flowsList) { it.toSet() }
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


    private fun trackPresence() {
        viewModelScope.launch {
            updatePresenceFlow
                .debounce(500L)
                .collect {
                    try {
                        Timber.d("Signaling presence due to update presence flow")
                        retryConnectionsAndSignalPresenceUseCase()
                    } catch (e: Exception) {
                        Timber.e(e, "Error signaling presence")
                    }
                }
        }
    }

    fun signalPresence() {
        updatePresenceFlow.update { !it }
    }

}
