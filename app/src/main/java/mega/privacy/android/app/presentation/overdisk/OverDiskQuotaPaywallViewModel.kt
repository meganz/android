package mega.privacy.android.app.presentation.overdisk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.GetSpecificAccountDetail
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import javax.inject.Inject

/**
 * Over disk quota paywall view model
 *
 */
@HiltViewModel
class OverDiskQuotaPaywallViewModel @Inject constructor(
    private val isDatabaseEntryStale: IsDatabaseEntryStale,
    private val getSpecificAccountDetail: GetSpecificAccountDetail,
) : ViewModel() {
    /**
     * Request storage details only if is not already requested recently
     */
    fun requestStorageDetailIfNeeded() {
        viewModelScope.launch {
            if (isDatabaseEntryStale()) {
                getSpecificAccountDetail(storage = true, transfer = false, pro = false)
            }
        }
    }
}