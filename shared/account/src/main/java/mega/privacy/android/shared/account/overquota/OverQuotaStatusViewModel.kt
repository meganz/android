package mega.privacy.android.shared.account.overquota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.MonitorAlmostFullStorageBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.SetAlmostFullStorageBannerClosingTimestampUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.android.shared.account.overquota.mapper.OverQuotaStatusMapper
import mega.privacy.android.shared.account.overquota.model.OverQuotaStatus
import mega.privacy.android.shared.account.overquota.model.OverQuotaStatusUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel that monitors and exposes the current over quota status.
 */
@HiltViewModel
class OverQuotaStatusViewModel @Inject constructor(
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorAlmostFullStorageBannerVisibilityUseCase: MonitorAlmostFullStorageBannerVisibilityUseCase,
    private val setAlmostFullStorageBannerClosingTimestampUseCase: SetAlmostFullStorageBannerClosingTimestampUseCase,
    private val overQuotaStatusMapper: OverQuotaStatusMapper,
) : ViewModel() {

    val uiState: StateFlow<OverQuotaStatusUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            overQuotaStatusFlow(),
            monitorAlmostFullStorageBannerVisibilityUseCase()
                .catch { Timber.e(it) },
        ) { overQuotaStatus, shouldShowWarning ->
            OverQuotaStatusUiState.Data(
                overQuotaStatus = overQuotaStatus,
                shouldShowWarning = shouldShowWarning,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, OverQuotaStatusUiState.Loading)
    }

    private fun overQuotaStatusFlow(): Flow<OverQuotaStatus> =
        combine(
            monitorStorageStateUseCase(),
            monitorTransferOverQuotaUseCase(),
            monitorAccountDetailUseCase()
                .map { it.levelDetail?.accountType?.isPaid == false },
        ) { storageState, transferOverQuota, isFreeAccount ->
            overQuotaStatusMapper(storageState, transferOverQuota, isFreeAccount)
        }

    /**
     * Dismiss the warning banner by recording the closing timestamp.
     */
    fun dismissWarning() {
        viewModelScope.launch {
            runCatching {
                setAlmostFullStorageBannerClosingTimestampUseCase()
            }.onFailure { Timber.e(it) }
        }
    }
}
