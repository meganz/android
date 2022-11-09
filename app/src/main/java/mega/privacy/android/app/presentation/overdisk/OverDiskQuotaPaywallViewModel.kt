package mega.privacy.android.app.presentation.overdisk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * Over disk quota paywall view model
 *
 */
@HiltViewModel
class OverDiskQuotaPaywallViewModel @Inject constructor(
    private val isDatabaseEntryStale: IsDatabaseEntryStale,
    @MegaApi private val megaApi: MegaApiAndroid
) : ViewModel() {
    /**
     * Request storage details only if is not already requested recently
     */
    fun requestStorageDetailIfNeeded() {
        viewModelScope.launch {
            if (isDatabaseEntryStale()) {
                megaApi.getSpecificAccountDetails(true, false, false)
            }
        }
    }
}