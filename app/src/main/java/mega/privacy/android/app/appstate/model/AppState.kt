package mega.privacy.android.app.appstate.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem

@Stable
sealed interface AppState {
    data object Loading : AppState

    data class Data(
        val mainNavItems: ImmutableSet<NavigationItem>,
        val featureDestinations: ImmutableSet<FeatureDestination>,
        val initialMainDestination: Any,
        val themeMode: ThemeMode,
    ) : AppState
}

class AppStateDataBuilder {
    private var mainNavItemsBuilder: Set<NavigationItem>? = null
    private val featureDestinationsBuilder = mutableSetOf<FeatureDestination>()
    private var initialDestinationBuilder: Any? = null
    private var themeModeBuilder: ThemeMode = ThemeMode.System

    fun mainNavItems(mainNavItems: Set<NavigationItem>) = this.apply {
        mainNavItemsBuilder = mainNavItems
    }

    fun featureDestinations(featureGraphs: Set<FeatureDestination>) = this.apply {
        featureDestinationsBuilder.addAll(featureGraphs)
    }

    fun initialDestination(destination: Any) = this.apply {
        initialDestinationBuilder = destination
    }

    fun themeMode(themeMode: ThemeMode) = this.apply {
        themeModeBuilder = themeMode
    }

    fun build(): AppState.Data {
        requireNotNull(mainNavItemsBuilder) { "Main nav items must be set" }
        require(mainNavItemsBuilder!!.isNotEmpty()) { "Main nav items cannot be empty" }

        return AppState.Data(
            mainNavItems = mainNavItemsBuilder!!.toImmutableSet(),
            featureDestinations = featureDestinationsBuilder.toImmutableSet(),
            initialMainDestination = initialDestinationBuilder
                ?: mainNavItemsBuilder!!.first().navItem.destination,
            themeMode = themeModeBuilder,
        )
    }
}