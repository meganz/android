package mega.privacy.android.app.appstate.model

import androidx.compose.runtime.Stable
import androidx.navigation.NavGraphBuilder
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem

@Stable
sealed interface AppState {
    data object Loading : AppState

    data class Data(
        val mainNavItems: ImmutableSet<NavigationItem>,
        val mainNavScreens: ImmutableSet<NavGraphBuilder.(navigationHandler: NavigationHandler, NavigationUiController) -> Unit>,
        val featureDestinations: ImmutableSet<FeatureDestination>,
        val initialMainDestination: NavKey,
    ) : AppState

    data object FetchingNodes : AppState
}

class AppStateDataBuilder {
    private var mainNavItemsBuilder: Set<NavigationItem>? = null
    private val mainNavScreensBuilder: MutableSet<NavGraphBuilder.(navigationHandler: NavigationHandler, NavigationUiController) -> Unit> =
        mutableSetOf()
    private val featureDestinationsBuilder = mutableSetOf<FeatureDestination>()
    private var initialDestinationBuilder: NavKey? = null

    fun mainNavItems(mainNavItems: Set<NavigationItem>) = this.apply {
        mainNavItemsBuilder = mainNavItems
    }

    fun mainNavScreens(mainNavScreens: Set<NavGraphBuilder.(navigationHandler: NavigationHandler, NavigationUiController) -> Unit>) =
        this.apply {
            mainNavScreensBuilder.addAll(mainNavScreens)
        }

    fun featureDestinations(featureGraphs: Set<FeatureDestination>) = this.apply {
        featureDestinationsBuilder.addAll(featureGraphs)
    }

    fun initialDestination(destination: NavKey) = this.apply {
        initialDestinationBuilder = destination
    }

    fun build(): AppState.Data {
        requireNotNull(mainNavItemsBuilder) { "Main nav items must be set" }
        require(mainNavItemsBuilder!!.isNotEmpty()) { "Main nav items cannot be empty" }

        return AppState.Data(
            mainNavItems = mainNavItemsBuilder!!.toImmutableSet(),
            mainNavScreens = mainNavScreensBuilder.toImmutableSet(),
            featureDestinations = featureDestinationsBuilder.toImmutableSet(),
            initialMainDestination = initialDestinationBuilder
                ?: mainNavItemsBuilder!!.first().destination,
        )
    }
}