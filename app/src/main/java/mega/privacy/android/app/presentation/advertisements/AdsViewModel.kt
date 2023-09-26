package mega.privacy.android.app.presentation.advertisements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.advertisements.model.AdsLoadState
import mega.privacy.android.domain.entity.advertisements.FetchAdDetailRequest
import mega.privacy.android.domain.usecase.advertisements.FetchAdDetailUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for Ads related data
 */
@HiltViewModel
class AdsViewModel @Inject constructor(
    private val fetchAdDetailUseCase: FetchAdDetailUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<AdsLoadState>(AdsLoadState.Empty)

    /**
     * Ads state
     */
    val state = _state.asStateFlow()


    /**
     * Update AdsLoadState to Loaded when ad url is fetched
     */
    fun fetchAdUrl(
        slotId: String,
        linkHandle: Long? = null,
    ) {
        val fetchAdDetailRequest = FetchAdDetailRequest(slotId, linkHandle)
        viewModelScope.launch {
            runCatching {
                fetchAdDetailUseCase(fetchAdDetailRequest)
            }.onFailure {
                Timber.w(it)
                _state.emit(AdsLoadState.Empty)
            }.onSuccess {
                _state.emit(AdsLoadState.Loaded(it.url))
            }
        }
    }
}