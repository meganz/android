package mega.privacy.android.app.appstate.content.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.app.appstate.content.navigation.model.StorageStatusUiState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StorageStatusViewModel @Inject constructor(
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
) : ViewModel() {

    val state: StateFlow<StorageStatusUiState> by lazy {
        monitorStorageStateUseCase()
            .onStart { emit(StorageState.Unknown) }
            .distinctUntilChanged()
            .map { storageState ->
                StorageStatusUiState(storageState = storageState)
            }
            .catch { e ->
                Timber.e(e, "Error monitoring storage state")
            }
            .asUiStateFlow(
                scope = viewModelScope,
                initialValue = StorageStatusUiState(storageState = StorageState.Unknown)
            )
    }
}

