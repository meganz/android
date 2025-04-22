package mega.privacy.android.app.presentation.hidenode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.SetHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class HiddenNodesOnboardingViewModel @Inject constructor(
    private val setHiddenNodesOnboardedUseCase: SetHiddenNodesOnboardedUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : ViewModel() {
    private val _state: MutableStateFlow<HiddenNodesOnboardingState> =
        MutableStateFlow(HiddenNodesOnboardingState())
    val state: StateFlow<HiddenNodesOnboardingState> = _state.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() = viewModelScope.launch {
        runCatching {
            val accountType =
                monitorAccountDetailUseCase().firstOrNull()?.levelDetail?.accountType
            val businessStatus = if (accountType?.isBusinessAccount == true) {
                getBusinessStatusUseCase()
            } else null

            _state.update {
                it.copy(
                    isInitialized = true,
                    accountType = accountType,
                    isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired,
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    fun setHiddenNodesOnboarded() = viewModelScope.launch {
        runCatching {
            setHiddenNodesOnboardedUseCase()
        }.onFailure {
            Timber.e(it)
        }
    }
}
