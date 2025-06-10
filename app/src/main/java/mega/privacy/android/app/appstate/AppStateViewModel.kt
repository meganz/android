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
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.navigation.GetStartScreenPreferenceDestinationUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val mainDestinations: Set<@JvmSuppressWildcards MainNavItem>,
    private val featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getStartScreenPreferenceDestinationUseCase: GetStartScreenPreferenceDestinationUseCase,
) : ViewModel() {

    val state: StateFlow<AppState> by lazy {
        combine(
            getFilteredValues(mainDestinations)
                .log("Main Destinations"),
            getFilteredValues(featureDestinations)
                .log("Feature Destinations"),
            getStartScreenPreferenceDestinationUseCase()
                .log("Start Screen Preference Destination")
        ) { mainItems, featureItems, startDestination ->
            AppStateDataBuilder()
                .mainNavItems(mainItems)
                .featureDestinations(featureItems)
                .initialDestination(startDestination)
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

}
