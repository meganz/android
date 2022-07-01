package mega.privacy.android.app.presentation.transfers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.entity.TransfersInfo
import mega.privacy.android.app.domain.usecase.GetNumPendingDownloadsNonBackground
import mega.privacy.android.app.domain.usecase.GetNumPendingTransfers
import mega.privacy.android.app.domain.usecase.GetNumPendingUploads
import mega.privacy.android.app.domain.usecase.IsCompletedTransfersEmpty
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import javax.inject.Inject

/**
 * ViewModel for managing transfers data.
 *
 * @property getNumPendingDownloadsNonBackground    [GetNumPendingDownloadsNonBackground]
 * @property getNumPendingUploads                   [GetNumPendingUploads]
 * @property getNumPendingTransfers                 [GetNumPendingTransfers]
 * @property isCompletedTransfersEmpty              [IsCompletedTransfersEmpty]
 */
@HiltViewModel
class TransfersManagementViewModel @Inject constructor(
    private val getNumPendingDownloadsNonBackground: GetNumPendingDownloadsNonBackground,
    private val getNumPendingUploads: GetNumPendingUploads,
    private val getNumPendingTransfers: GetNumPendingTransfers,
    private val isCompletedTransfersEmpty: IsCompletedTransfersEmpty,
) : ViewModel() {

    private val transfersInfo: MutableLiveData<Pair<Int, TransfersInfo>> = MutableLiveData()
    private val shouldShowCompletedTab = SingleLiveEvent<Boolean>()

    /**
     * Notifies about updates on Transfers info.
     */
    fun onTransfersInfoUpdate(): LiveData<Pair<Int, TransfersInfo>> = transfersInfo

    /**
     * Notifies about updates on if should show or not the Completed tab.
     */
    fun onGetShouldCompletedTab(): LiveData<Boolean> = shouldShowCompletedTab

    /**
     * Checks transfers info.
     */
    fun checkTransfersInfo(transferType: Int) {
        viewModelScope.launch {
            val numPendingDownloadsNonBackground = getNumPendingDownloadsNonBackground()
            val numPendingUploads = getNumPendingUploads()

            transfersInfo.value = Pair(
                transferType,
                TransfersInfo(numPendingDownloadsNonBackground, numPendingUploads)
            )
        }
    }

    /**
     * Checks if should show the Completed tab or not.
     */
    fun checkIfShouldShowCompletedTab() {
        viewModelScope.launch {
            shouldShowCompletedTab.value =
                !isCompletedTransfersEmpty() && getNumPendingTransfers() <= 0
        }
    }
}