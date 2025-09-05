package mega.privacy.android.app.main.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.billing.GetRecommendedSubscriptionUseCase
import mega.privacy.android.feature.payment.model.mapper.LocalisedSubscriptionMapper
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class AdsFreeIntroViewModel @Inject constructor(
    private val getRecommendedSubscriptionUseCase: GetRecommendedSubscriptionUseCase,
    private val localisedSubscriptionMapper: LocalisedSubscriptionMapper,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AdsFreeIntroUiState())

    /**
     * Ads Free Intro UI state
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                getRecommendedSubscriptionUseCase()?.let {
                    localisedSubscriptionMapper(it, it)
                }
            }.onSuccess { cheapestSubscription ->
                _state.update { it.copy(cheapestSubscriptionAvailable = cheapestSubscription) }
            }.onFailure {
                Timber.e(it)
            }
        }

        viewModelScope.launch {
            monitorThemeModeUseCase().catch {
                Timber.e(it)
            }.collect { themeMode ->
                _state.update { it.copy(themeMode = themeMode) }
            }
        }
    }
}