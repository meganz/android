package mega.privacy.android.app.presentation.meeting.chat.view.message.normal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.chat.RichLinkConfig
import mega.privacy.android.domain.usecase.chat.link.EnableRichPreviewUseCase
import mega.privacy.android.domain.usecase.chat.link.MonitorRichLinkPreviewConfigUseCase
import mega.privacy.android.domain.usecase.chat.link.SetRichLinkWarningCounterUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Chat message text view model
 *
 */
@HiltViewModel
class ChatMessageTextViewModel @Inject constructor(
    private val monitorRichLinkPreviewConfigUseCase: MonitorRichLinkPreviewConfigUseCase,
    private val setRichLinkWarningCounterUseCase: SetRichLinkWarningCounterUseCase,
    private val enableRichLinkPreviewUseCase: EnableRichPreviewUseCase,
) : ViewModel() {

    /**
     * Rich link config
     */
    val richLinkConfig = monitorRichLinkPreviewConfigUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), RichLinkConfig())

    /**
     * Set rich link warning counter
     *
     */
    fun setRichLinkWarningCounter(counter: Int) {
        viewModelScope.launch {
            runCatching {
                setRichLinkWarningCounterUseCase(counter)
            }.onFailure {
                Timber.e(it, "Failed to set rich link warning counter")
            }
        }
    }

    /**
     * Enable rich link preview
     */
    fun enableRichLinkPreview(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                enableRichLinkPreviewUseCase(enabled)
            }.onFailure {
                Timber.e(it, "Failed to enable rich link preview")
            }
        }
    }
}