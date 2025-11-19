package mega.privacy.android.app.presentation.myaccount.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.myaccount.model.MyAccountHomeUIState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import javax.inject.Inject

@HiltViewModel
class SingleCardExampleWidgetViewModel @Inject constructor() : ViewModel() {
    private var accountDetail = MyAccountHomeUIState(
        name = "John Doe",
        accountType = null,
        accountDetail = AccountDetail(
            storageDetail = AccountStorageDetail(
                usedStorage = 100L,
                totalStorage = 1_000,
                usedCloudDrive = 0L,
                usedRubbish = 0L,
                usedIncoming = 0L,
                subscriptionMethodId = 0,
            ),
        ),
        accountTypeNameResource = R.string.pro2_account,
    )

    val uiState: StateFlow<MyAccountHomeUIState> by lazy {
        flow {
            repeat(9) {
                delay(1000)
                val accountDetails = accountDetail.accountDetail
                accountDetail = accountDetail.copy(
                    accountDetail = accountDetails?.copy(
                        storageDetail = accountDetails.storageDetail?.copy(
                            usedStorage = accountDetails.storageDetail?.usedStorage?.plus(
                                100L
                            ) ?: 0L
                        )
                    )
                )
                emit(accountDetail)
            }
        }.asUiStateFlow(viewModelScope, accountDetail)
    }
}