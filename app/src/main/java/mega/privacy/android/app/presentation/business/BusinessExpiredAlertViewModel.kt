package mega.privacy.android.app.presentation.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.presentation.business.model.BusinessExpiredAlertUiState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.usecase.IsMasterBusinessAccountUseCase
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import mega.privacy.android.core.coroutine.asUiStateFlow
import javax.inject.Inject

/**
 * ViewModel for the Business Expired Alert screen
 */
@HiltViewModel
class BusinessExpiredAlertViewModel @Inject constructor(
    private val getAccountTypeUseCase: GetAccountTypeUseCase,
    private val isMasterBusinessAccountUseCase: IsMasterBusinessAccountUseCase,
) : ViewModel() {

    /**
     * UI state
     */
    val uiState: StateFlow<BusinessExpiredAlertUiState> by lazy {
        combine(
            flow { emit(getAccountTypeUseCase()) }.catch { emit(AccountType.UNKNOWN) },
            flow { emit(isMasterBusinessAccountUseCase()) }.catch { emit(false) },
        ) { accountType, isMaster ->
            BusinessExpiredAlertUiState(
                isProFlexiAccount = accountType == AccountType.PRO_FLEXI,
                isMasterBusinessAccount = isMaster,
            )
        }.asUiStateFlow(viewModelScope, BusinessExpiredAlertUiState())
    }
}
