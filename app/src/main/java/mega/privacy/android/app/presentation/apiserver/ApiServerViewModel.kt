package mega.privacy.android.app.presentation.apiserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.apiserver.model.ApiServerUIState
import mega.privacy.android.domain.entity.apiserver.ApiServer
import mega.privacy.android.domain.usecase.apiserver.GetCurrentApiServerUseCase
import mega.privacy.android.domain.usecase.apiserver.UpdateApiServerUseCase
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
) : ViewModel() {

    private val _state = MutableStateFlow(ApiServerUIState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val currentApiServer = getCurrentApiServerUseCase()
            _state.update { state -> state.copy(currentApiServer = currentApiServer) }
        }
    }

    /**
     * Update api server
     *
     * @param apiServer
     */
    fun updateNewApiServer(apiServer: ApiServer) {
        _state.update { state -> state.copy(newApiServer = apiServer) }
    }

    /**
     * Updates api server
     */
    fun confirmUpdateApiServer() {
        viewModelScope.launch {
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