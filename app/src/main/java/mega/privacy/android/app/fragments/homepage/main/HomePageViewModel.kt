package mega.privacy.android.app.fragments.homepage.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.banner.Banner
import mega.privacy.android.domain.usecase.banner.DismissBannerUseCase
import mega.privacy.android.domain.usecase.banner.GetBannersUseCase
import mega.privacy.android.domain.usecase.contact.MonitorMyChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.notifications.MonitorHomeBadgeCountUseCase
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomePageViewModel @Inject constructor(
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val monitorHomeBadgeCountUseCase: MonitorHomeBadgeCountUseCase,
    private val monitorMyChatOnlineStatusUseCase: MonitorMyChatOnlineStatusUseCase,
    private val getBannersUseCase: GetBannersUseCase,
    private val dismissBannerUseCase: DismissBannerUseCase,
) : ViewModel() {
    private val _notificationCount = MutableLiveData<Int>()
    private val _banners = MutableStateFlow(emptyList<Banner>())
    val banners = _banners.asStateFlow()

    val notificationCount: LiveData<Int> = _notificationCount

    private val _uiState = MutableStateFlow(HomePageUiState())

    /**
     * State of the home page
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Is network connected state
     */
    val isConnected =
        monitorConnectivityUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Monitor Internet Connectivity
     */
    @OptIn(FlowPreview::class)
    val monitorConnectivity = monitorConnectivityUseCase().debounce(TimeUnit.SECONDS.toMillis(1))

    /**
     * Monitor Fetch Nodes finish
     */
    val monitorFetchNodesFinish = monitorFetchNodesFinishUseCase()

    init {
        viewModelScope.launch {
            monitorHomeBadgeCountUseCase().conflate().collect {
                _notificationCount.value = it
            }
        }
        viewModelScope.launch {
            monitorMyChatOnlineStatusUseCase()
                .catch { Timber.e(it) }
                .collect { onlineStatus ->
                    _uiState.update { state -> state.copy(userChatStatus = onlineStatus.status) }
                }
        }
    }

    /**
     * Get banner list from the server or from memory cache
     */
    fun getBanners() {
        viewModelScope.launch {
            runCatching {
                getBannersUseCase()
            }.onSuccess { banners ->
                _banners.update { banners }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Dismiss the banner for this account.
     * The banner would never be given again by the server once being dismissed
     */
    fun dismissBanner(banner: Banner) {
        viewModelScope.launch {
            runCatching {
                dismissBannerUseCase(banner.id)
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}
