package mega.privacy.android.app.appstate.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableSet
import mega.privacy.android.navigation.contract.FeatureDestination

@Stable
sealed interface AppState {
    data object Loading : AppState

    data class Data(
        val featureDestinations: ImmutableSet<FeatureDestination>,
    ) : AppState

    data object FetchingNodes : AppState
}