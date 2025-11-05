package mega.privacy.android.app.appstate.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.appstate.content.model.NavigationGraphState
import mega.privacy.android.domain.usecase.featureflag.GetEnabledFlaggedItemsUseCase
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class NavigationGraphViewModel @Inject constructor(
    private val featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>,
    private val getEnabledFlaggedItemsUseCase: GetEnabledFlaggedItemsUseCase,
    private val appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>,
) : ViewModel() {

    val state: StateFlow<NavigationGraphState> by lazy {
        getEnabledFlaggedItemsUseCase(featureDestinations)
            .log("Feature Destinations").map { featureItems ->
                NavigationGraphState.Data(
                    featureDestinations = featureItems.toImmutableSet(),
                    appDialogDestinations = appDialogDestinations.toImmutableSet(),
                )
            }.catch {
                Timber.e(it, "Error while building app state")
            }.distinctUntilChanged()
            .onEach {
                Timber.d("AppState emitted: $it")
            }.asUiStateFlow(
                scope = viewModelScope,
                initialValue = NavigationGraphState.Loading
            )
    }

    private fun <T> Flow<T>.log(flowName: String): Flow<T> = this.onEach {
        Timber.d("$flowName emitted: $it")
    }
}