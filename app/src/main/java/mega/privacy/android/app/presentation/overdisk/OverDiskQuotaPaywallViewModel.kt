package mega.privacy.android.app.presentation.overdisk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.overdisk.model.OverDiskQuotaPaywallUiState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.account.GetNumberOfNodesUseCase
import mega.privacy.android.domain.usecase.account.GetOverDiskQuotaDeadlineUseCase
import mega.privacy.android.domain.usecase.account.GetOverDiskQuotaWarningTimestampsUseCase
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.GetUserDataUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import timber.log.Timber
import javax.inject.Inject

/**
 * Over disk quota paywall view model.
 *
 * Exposes a single [OverDiskQuotaPaywallUiState] built from domain use cases, plus the current
 * [themeMode] used to theme the screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OverDiskQuotaPaywallViewModel @Inject constructor(
    private val isDatabaseEntryStale: IsDatabaseEntryStale,
    private val getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase,
    private val getPricing: GetPricing,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val getNumberOfNodesUseCase: GetNumberOfNodesUseCase,
    private val getOverDiskQuotaDeadlineUseCase: GetOverDiskQuotaDeadlineUseCase,
    private val getOverDiskQuotaWarningTimestampsUseCase: GetOverDiskQuotaWarningTimestampsUseCase,
    monitorUpdateUserDataUseCase: MonitorUpdateUserDataUseCase,
    monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase,
    monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    monitorThemeModeUseCase: MonitorThemeModeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverDiskQuotaPaywallUiState())

    /**
     * UI state for the Over Disk Quota Paywall screen.
     */
    val uiState: StateFlow<OverDiskQuotaPaywallUiState> = _uiState.asStateFlow()

    /**
     * Current theme mode, used to theme the screen.
     */
    val themeMode: StateFlow<ThemeMode> = monitorThemeModeUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)

    init {
        viewModelScope.launch {
            val pricing = runCatching { getPricing(false) }.getOrElse { Pricing(emptyList()) }
            _uiState.update { it.copy(products = pricing.products) }
        }

        monitorAccountDetailUseCase()
            .mapNotNull { it.storageDetail?.usedStorage }
            .onEach { used -> _uiState.update { it.copy(usedStorage = used) } }
            .launchIn(viewModelScope)

        merge(
            monitorUpdateUserDataUseCase(),
            monitorMyAccountUpdateUseCase().map { },
        ).onEach {
            refreshAccountData()
        }.launchIn(viewModelScope)

        requestStorageDetailIfNeeded()
        getUserData()
    }

    /**
     * Refreshes the account-derived fields of the UI state (email, file count, over quota
     * warning timestamps and deletion deadline).
     */
    private fun refreshAccountData() {
        viewModelScope.launch {
            val email = runCatching { getCurrentUserEmail() }.getOrNull().orEmpty()
            val fileCount = runCatching { getNumberOfNodesUseCase() }.getOrDefault(0L)
            val warnings = runCatching { getOverDiskQuotaWarningTimestampsUseCase() }
                .getOrDefault(emptyList())
            val deadline = runCatching { getOverDiskQuotaDeadlineUseCase() }.getOrDefault(-1L)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    email = email,
                    fileCount = fileCount,
                    warningTimestamps = warnings,
                    deadlineTimestamp = deadline,
                )
            }
        }
    }

    /**
     * Requests storage details only if not already requested recently.
     */
    private fun requestStorageDetailIfNeeded() {
        viewModelScope.launch {
            if (isDatabaseEntryStale()) {
                runCatching {
                    getSpecificAccountDetailUseCase(storage = true, transfer = false, pro = false)
                }.onFailure {
                    Timber.w("Exception getting account detail: $it")
                }
            }
        }
    }

    /**
     * Requests the user's data.
     */
    private fun getUserData() {
        viewModelScope.launch {
            runCatching {
                getUserDataUseCase()
            }.onFailure {
                Timber.e("Failed to get the user's data: $it")
            }
        }
    }
}
