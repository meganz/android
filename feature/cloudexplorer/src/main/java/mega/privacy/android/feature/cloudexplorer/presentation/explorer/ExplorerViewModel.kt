package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ExplorerViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) : ViewModel() {

    private val tabSignals = MutableStateFlow<Map<Int, TabSignal>>(emptyMap())
    private val selectedTab = MutableStateFlow(CLOUD_TAB_INDEX)
    private val noConnectionChannel = Channel<StateEvent>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            if (!monitorConnectivityUseCase().first()) noConnectionChannel.trySend(triggered)
        }
    }


    val uiState: StateFlow<ExplorerUiState> =
        combine(
            tabSignals,
            selectedTab,
            monitorConnectivityUseCase().catch { Timber.e(it) }.onStart { emit(true) },
            noConnectionChannel.receiveAsFlow().onStart { emit(consumed) },
        ) { signals, tab, isConnected, noConnectionEvent ->
            val active = signals[tab] ?: TabSignal()
            ExplorerUiState(
                isLoading = active.isLoading,
                hasContent = active.hasContent,
                folderName = active.folderName,
                isConnected = isConnected,
                noConnectionEvent = noConnectionEvent,
            )
        }.asUiStateFlow(viewModelScope, ExplorerUiState())

    fun onTabSelected(index: Int) {
        selectedTab.update { index }
    }

    fun onTabSignal(index: Int, signal: TabSignal) {
        tabSignals.update { it + (index to signal) }
    }

    fun onNoConnectionEventConsumed() {
        noConnectionChannel.trySend(consumed)
    }
}
