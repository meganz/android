package mega.privacy.android.app.presentation.apiserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.apiserver.model.ApiServerUIState
import mega.privacy.android.domain.entity.apiserver.ApiServer
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.apiserver.GetCurrentApiServerUseCase
import mega.privacy.android.domain.usecase.apiserver.UpdateApiServerUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Api server view model
 *
 * @property getCurrentApiServerUseCase [GetCurrentApiServerUseCase]
 * @property updateApiServerUseCase [UpdateApiServerUseCase]
 * @property state [ApiServerUIState]
 */
@HiltViewModel
class ApiServerViewModel @Inject constructor(
    private val getCurrentApiServerUseCase: GetCurrentApiServerUseCase,
    private val updateApiServerUseCase: UpdateApiServerUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val newApiServerFlow = MutableStateFlow<ApiServer?>(null)

    internal val state: StateFlow<ApiServerUIState> by lazy {
        combine(
            flow { emit(getCurrentApiServerUseCase()) }.catch { Timber.e(it) },
            flow {
                emit(getFeatureFlagValueUseCase(AppFeatures.SingleActivity))
            }.catch { Timber.e(it) },
            newApiServerFlow
        ) { currentApiServer, isSingleActivityEnabled, newApiServer ->
            ApiServerUIState(
                currentApiServer = currentApiServer,
                newApiServer = newApiServer,
                isSingleActivityEnabled = isSingleActivityEnabled
            )
        }.catch {
            Timber.e(it)
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = ApiServerUIState()
        )
    }

    /**
     * Update api server
     *
     * @param apiServer
     */
    fun updateNewApiServer(apiServer: ApiServer) {
        newApiServerFlow.update { apiServer }
    }

    /**
     * Updates api server
     */
    fun confirmUpdateApiServer() {
        applicationScope.launch {
            with(state.value) {
                runCatching {
                    if (currentApiServer != null && newApiServer != null) {
                        updateApiServerUseCase(currentApiServer, newApiServer)
                    }
                }.onSuccess { Timber.d("Api server updated from $currentApiServer to $newApiServer") }
                    .onFailure { Timber.e(it) }
            }
        }
    }
}