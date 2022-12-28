package mega.privacy.android.app.presentation.rubbishbin

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.app.presentation.inbox.model.InboxState
import mega.privacy.android.app.presentation.rubbishbin.model.RubbishBinState
import javax.inject.Inject

/**
 * [ViewModel] class associated to RubbishBinFragment
 *
 */
@HiltViewModel
class RubbishBinViewModel @Inject constructor(
) : ViewModel() {

    /**
     * The RubbishBin UI State
     */
    private val _state = MutableStateFlow(RubbishBinState())

    /**
     * The RubbishBin UI State accessible outside the ViewModel
     */
    val state: StateFlow<RubbishBinState> = _state
}