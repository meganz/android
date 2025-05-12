package mega.privacy.android.app.main.dialog.businessgrace

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.usecase.account.MonitorUnverifiedBusinessAccountUseCase
import javax.inject.Inject

/**
 * Business account view model
 */
@HiltViewModel
class BusinessAccountViewModel @Inject constructor(
    private val monitorUnverifiedBusinessAccountUseCase: MonitorUnverifiedBusinessAccountUseCase,
) : ViewModel() {
    /**
     * Monitor unverified business account
     */
    val unverifiedBusinessAccountState = monitorUnverifiedBusinessAccountUseCase()
}