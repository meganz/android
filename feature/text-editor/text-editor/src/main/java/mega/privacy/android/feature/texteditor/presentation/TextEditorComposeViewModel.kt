package mega.privacy.android.feature.texteditor.presentation

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorComposeUiState
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarAction
import mega.privacy.android.feature.texteditor.presentation.model.DefaultTextEditorTopBarSlots
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarSlots

/**
 * ViewModel for the Compose text editor screen.
 * Uses MVI-style intent handling: UI emits actions via [onMenuAction], ViewModel processes them.
 */
@HiltViewModel(assistedFactory = TextEditorComposeViewModel.Factory::class)
class TextEditorComposeViewModel @AssistedInject constructor(
    @Assisted val args: Args,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TextEditorComposeUiState(
            fileName = args.fileName.orEmpty(),
            mode = args.mode,
            isLoading = false,
            topBarSlots = args.topBarSlots,
        )
    )
    val uiState = _uiState.asStateFlow()

    @AssistedFactory
    interface Factory {
        fun create(args: Args): TextEditorComposeViewModel
    }

    data class Args(
        val nodeHandle: Long,
        val mode: TextEditorMode,
        val nodeSourceType: Int?,
        val fileName: String?,
        val topBarSlots: TextEditorTopBarSlots = DefaultTextEditorTopBarSlots,
    )

    fun setViewMode() {
        _uiState.update {
            it.copy(mode = TextEditorMode.View)
        }
    }

    fun saveFile(fromHome: Boolean) {
    }

    /**
     * Handles top bar action intents from the UI (MVI pattern).
     */
    fun onMenuAction(action: TextEditorTopBarAction) {
        when (action) {
            TextEditorTopBarAction.Download -> {}
            TextEditorTopBarAction.GetLink -> {}
            TextEditorTopBarAction.SendToChat -> {}
            TextEditorTopBarAction.Share -> {}
            TextEditorTopBarAction.LineNumbers -> {
                _uiState.update {
                    it.copy(showLineNumbers = !it.showLineNumbers)
                }
            }
        }
    }
}
