package mega.privacy.android.feature.sharelink.presentation

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the revamped Share link screen.
 *
 * MR0 foundation stub — assisted-injected with the node [Args.handles], mirroring
 * `FileLinkViewModel`. MR1 (AND-24035) wires in the existing export / account-type
 * use cases to load and expose the real link details.
 */
@HiltViewModel(assistedFactory = ShareLinkViewModel.Factory::class)
class ShareLinkViewModel @AssistedInject constructor(
    @Assisted private val args: Args,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ShareLinkUiState(handles = args.handles, isLoading = false)
    )

    /**
     * Share link UI state.
     */
    val uiState: StateFlow<ShareLinkUiState> = _uiState.asStateFlow()

    /**
     * Assisted factory arguments.
     *
     * @property handles Node handles whose link is being shared.
     */
    data class Args(val handles: List<Long>)

    /**
     * Assisted factory for [ShareLinkViewModel].
     */
    @AssistedFactory
    interface Factory {
        /**
         * Create a [ShareLinkViewModel] for the given [args].
         */
        fun create(args: Args): ShareLinkViewModel
    }
}
