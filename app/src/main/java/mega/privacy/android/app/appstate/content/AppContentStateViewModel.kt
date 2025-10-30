package mega.privacy.android.app.appstate.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.app.appstate.content.model.AppContentState
import mega.privacy.android.domain.entity.permission.OnboardingPermissionsCheckResult
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetEnabledFlaggedItemsUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.permisison.CheckOnboardingPermissionsUseCase
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class AppContentStateViewModel @Inject constructor(
    private val featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>,
    private val getEnabledFlaggedItemsUseCase: GetEnabledFlaggedItemsUseCase,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>,
    private val checkOnboardingPermissionsUseCase: CheckOnboardingPermissionsUseCase,
) : ViewModel() {

    val state: StateFlow<AppContentState> by lazy {
        combine(
            getEnabledFlaggedItemsUseCase(featureDestinations)
                .log("Feature Destinations"),
            monitorFetchNodesFinishUseCase()
                .onStart {
                    emit(rootNodeExistsUseCase())
                }
                .catch { Timber.e(it, "Error monitoring fetch nodes finish") }
                .log("Fetch Nodes Finish"),

            onboardingPermissionsCheckFlow()
        ) { featureItems, rootNodeExist, permissionsResult ->
            if (rootNodeExist) {
                AppContentState.Data(
                    featureDestinations = featureItems.toImmutableSet(),
                    appDialogDestinations = appDialogDestinations.toImmutableSet(),
                    onboardingPermissionsCheckResult = permissionsResult
                )
            } else {
                AppContentState.FetchingNodes
            }
        }.catch {
            Timber.e(it, "Error while building app state")
        }.distinctUntilChanged()
            .onEach {
                Timber.d("AppState emitted: $it")
            }.asUiStateFlow(
                scope = viewModelScope,
                initialValue = AppContentState.Loading
            )
    }

    private fun onboardingPermissionsCheckFlow(): Flow<OnboardingPermissionsCheckResult?> =
        flow<OnboardingPermissionsCheckResult?> { emit(checkOnboardingPermissionsUseCase()) }.catch { e ->
            Timber.e(e, "Error checking onboarding permissions")
            emit(null)
        }

    private fun <T> Flow<T>.log(flowName: String): Flow<T> = this.onEach {
        Timber.d("$flowName emitted: $it")
    }
}