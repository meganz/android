package mega.privacy.android.app.appstate.content.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableSet
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.FeatureDestination

@Stable
sealed interface AppContentState {
    data object Loading : AppContentState

    data class Data(
        val featureDestinations: ImmutableSet<FeatureDestination>,
        val appDialogDestinations: ImmutableSet<AppDialogDestinations>
    ) : AppContentState

    data object FetchingNodes : AppContentState
}