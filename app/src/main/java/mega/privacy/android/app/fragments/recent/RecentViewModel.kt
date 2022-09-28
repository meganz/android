package mega.privacy.android.app.fragments.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.presentation.recents.model.RecentsItem
import mega.privacy.android.app.domain.usecase.GetRecentActions
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

@HiltViewModel
class RecentViewModel @Inject constructor(
    getRecentActions: GetRecentActions,
) : ViewModel() {

    private val _buckets = MutableStateFlow<List<MegaRecentActionBucket>>(emptyList())
    val buckets = _buckets.asStateFlow()

    private val visibleContacts = ArrayList<MegaContactAdapter>()
    private val bucketSelected: MegaRecentActionBucket? = null
    private val recentsItems: ArrayList<RecentsItem>? = null

    init {
        viewModelScope.launch {
            _buckets.emit(getRecentActions())
        }
    }

}