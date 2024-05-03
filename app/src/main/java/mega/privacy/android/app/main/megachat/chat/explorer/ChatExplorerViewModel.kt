package mega.privacy.android.app.main.megachat.chat.explorer

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.app.main.model.chat.explorer.ChatExplorerUiState
import javax.inject.Inject

/**
 * View model class for [ChatExplorerFragment]
 */
@HiltViewModel
class ChatExplorerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ChatExplorerUiState())

    /**
     * The public property of [ChatExplorerUiState].
     */
    val uiState = _uiState.asStateFlow()
}
