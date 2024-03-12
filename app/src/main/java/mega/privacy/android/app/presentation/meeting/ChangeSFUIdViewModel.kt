package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.meeting.SetSFUIdUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Change SFU Id
 */
@HiltViewModel
class ChangeSFUIdViewModel @Inject constructor(
    private val setSFUIdUseCase: SetSFUIdUseCase,
) : ViewModel() {

    /**
     * Change the SFU Id
     * @param sfuId New SFU Id
     */
    fun changeSFUId(sfuId: Int) {
        viewModelScope.launch {
            runCatching {
                setSFUIdUseCase(sfuId)
                Timber.d("SFU Id changed to $sfuId")
            }.onFailure {
                Timber.e("Error changing SFU Id: $it")
            }
        }
    }
}
