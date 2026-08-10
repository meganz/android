package mega.privacy.mobile.home.presentation.home.widget.banner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.banner.DismissBannerUseCase
import mega.privacy.android.domain.usecase.banner.GetPromoBannersUseCase
import mega.privacy.mobile.home.presentation.home.widget.banner.model.BannerUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for banner widget
 */
@HiltViewModel
class BannerWidgetViewModel @Inject constructor(
    private val getPromoBannersUseCase: GetPromoBannersUseCase,
    private val dismissBannerUseCase: DismissBannerUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BannerUiState())
    val uiState: StateFlow<BannerUiState> = _uiState.asStateFlow()

    init {
        loadBanners()
    }

    /**
     * Load banners
     */
    private fun loadBanners() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }

            runCatching {
                getPromoBannersUseCase()
            }.onSuccess { bannerList ->
                _uiState.update {
                    it.copy(
                        banners = bannerList,
                        isLoading = false
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception, "Failed to load banners")
                _uiState.update {
                    it.copy(
                        banners = emptyList(),
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Dismiss a banner
     * @param bannerId The ID of the banner to dismiss
     */
    fun dismissBanner(bannerId: Int) {
        viewModelScope.launch {
            runCatching {
                dismissBannerUseCase(bannerId)
            }.onSuccess {
                // Remove dismissed banner from state
                _uiState.update { currentState ->
                    currentState.copy(
                        banners = currentState.banners.filter { it.id != bannerId }
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception, "Failed to dismiss banner $bannerId")
            }
        }
    }
}
