package mega.privacy.android.app.presentation.fingerprintauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.UpgradeSecurity
import javax.inject.Inject

/**
 * ViewModel associated with [SecurityUpgradeDialogFragment] responsible to call account security related functions
 */
@HiltViewModel
class SecurityUpgradeViewModel @Inject constructor(
    private val upgradeSecurity: UpgradeSecurity,
) : ViewModel() {

    /**
     * Function to upgrade account security
     */
    fun upgradeAccountSecurity() {
        viewModelScope.launch {
            upgradeSecurity()
        }
    }
}