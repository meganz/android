package mega.privacy.android.app.appstate.global.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.chat.MonitorOngoingCallUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel driving the global return-to-call banner in the app shell.
 *
 * Combines the ongoing call and theme mode monitors into a single [OngoingCallBannerUiState].
 */
@HiltViewModel
class OngoingCallBannerViewModel @Inject constructor(
    private val monitorOngoingCallUseCase: MonitorOngoingCallUseCase,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
) : ViewModel() {

    /**
     * UI state exposing the current call and theme mode for the banner.
     */
    val uiState: StateFlow<OngoingCallBannerUiState> by lazy {
        combine(
            monitorOngoingCallUseCase().catch {
                Timber.e(it, "Error monitoring ongoing call")
                emit(null)
            },
            monitorThemeModeUseCase().catch {
                Timber.e(it, "Error monitoring theme mode")
                emit(ThemeMode.System)
            },
        ) { currentCall, themeMode ->
            OngoingCallBannerUiState(currentCall = currentCall, themeMode = themeMode)
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = OngoingCallBannerUiState(
                currentCall = null,
                themeMode = ThemeMode.System,
            ),
        )
    }
}
