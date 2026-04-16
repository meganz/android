package mega.privacy.android.shared.ads.adsfreeintro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.formatter.mapper.FormattedPriceMapper
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.billing.GetRecommendedSubscriptionUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AdsFreeIntroViewModel @Inject constructor(
    private val getRecommendedSubscriptionUseCase: GetRecommendedSubscriptionUseCase,
    private val formattedPriceMapper: FormattedPriceMapper,
    private val formattedSizeMapper: FormattedSizeMapper,
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
                getRecommendedSubscriptionUseCase()
            }.onSuccess { subscription ->
                subscription?.let {
                    _state.update { state ->
                        state.copy(
                            formattedPrice = formattedPriceMapper(subscription.amount),
                            storageSize = formattedSizeMapper(
                                subscription.storage,
                                usePlaceholder = false,
                            ),
                        )
                    }
                }
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
