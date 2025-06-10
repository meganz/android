package mega.privacy.android.app.appstate.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem
import kotlin.reflect.KClass

@Stable
sealed interface AppState {
    data object Loading : AppState

    data class Data(
        val mainNavItems: ImmutableSet<MainNavItem>,
        val featureDestinations: ImmutableSet<FeatureDestination>,
        val initialDestination: KClass<*>,
    ) : AppState
}

class AppStateDataBuilder {
    private var mainNavItemsBuilder: Set<MainNavItem>? = null
    private val featureDestinationsBuilder = mutableSetOf<FeatureDestination>()
    private var initialDestinationBuilder: KClass<*>? = null

    fun mainNavItems(mainNavItems: Set<MainNavItem>) = this.apply {
        mainNavItemsBuilder = mainNavItems
    }

    fun featureDestinations(featureGraphs: Set<FeatureDestination>) = this.apply {
        featureDestinationsBuilder.addAll(featureGraphs)
    }

    fun initialDestination(destination: KClass<*>?) = this.apply {
        initialDestinationBuilder = destination
    }

    fun build(): AppState.Data {
        requireNotNull(mainNavItemsBuilder) { "Main nav items must be set" }
        require(mainNavItemsBuilder!!.isNotEmpty()) { "Main nav items cannot be empty" }

        return AppState.Data(
            mainNavItems = mainNavItemsBuilder!!.toImmutableSet(),
            featureDestinations = featureDestinationsBuilder.toImmutableSet(),
            initialDestination = initialDestinationBuilder
                ?: mainNavItemsBuilder!!.first().destinationClass
        )
    }
}