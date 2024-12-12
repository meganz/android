package mega.privacy.android.app.main.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedSubscriptionMapper
import mega.privacy.android.domain.usecase.billing.GetCheapestSubscriptionUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class AdsFreeIntroViewModel @Inject constructor(
    private val getCheapestSubscriptionUseCase: GetCheapestSubscriptionUseCase,
    private val localisedSubscriptionMapper: LocalisedSubscriptionMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(AdsFreeIntroUiState())

    /**
     * Ads Free Intro UI state
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                getCheapestSubscriptionUseCase().let {
                    localisedSubscriptionMapper(it, it)
                }
            }.onSuccess { cheapestSubscription ->
                _state.update { it.copy(cheapestSubscriptionAvailable = cheapestSubscription) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}