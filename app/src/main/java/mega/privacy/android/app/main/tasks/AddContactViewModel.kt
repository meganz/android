package mega.privacy.android.app.main.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.model.AddContactState
import mega.privacy.android.app.presentation.extensions.isOutShare
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasSensitiveDescendantUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * AddContactView Model
 * @param getContactVerificationWarningUseCase [GetFeatureFlagValueUseCase]
 * @param getFeatureFlagValueUseCase [GetFeatureFlagValueUseCase]
 * @param getChatCallUseCase [GetChatCallUseCase]
 * @param monitorChatCallUpdatesUseCase [MonitorChatCallUpdatesUseCase]
 */
@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val hasSensitiveDescendantUseCase: HasSensitiveDescendantUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddContactState())

    private val _sensitiveItemsCount: MutableStateFlow<Int?> = MutableStateFlow(null)
    val sensitiveItemsCountFlow = _sensitiveItemsCount.asStateFlow()

    /**
     * Ui State
     */
    val state = _state.asStateFlow()

    private var monitorChatCallJob: Job? = null

    init {
        getApiFeatureFlag()
        getContactFeatureEnabled()
    }

    /**
     * Get call unlimited pro plan api feature flag
     */
    private fun getApiFeatureFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.CallUnlimitedProPlan)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { flag ->
                _state.update { state ->
                    state.copy(
                        isCallUnlimitedProPlanFeatureFlagEnabled = flag,
                    )
                }
            }
        }
    }

    /**
     * Gets contact enabled Value from [GetContactVerificationWarningUseCase]
     */
    private fun getContactFeatureEnabled() {
        viewModelScope.launch {
            val contactVerificationWarningEnabled = getContactVerificationWarningUseCase()
            _state.update {
                it.copy(isContactVerificationWarningEnabled = contactVerificationWarningEnabled)
            }

        }
    }

    /**
     * Set chat id
     */
    fun setChatId(chatId: Long) {
        val id = chatId.takeIf { it != -1L } ?: return
        _state.update { it.copy(chatId = id) }
        getChatCall()
    }

    /**
     * Get chat call updates
     */
    private fun getChatCall() {
        _state.value.chatId?.let { chatId ->
            viewModelScope.launch {
                runCatching {
                    getChatCallUseCase(chatId)?.let { call ->
                        _state.update { it.copy(currentChatCall = call) }
                        monitorChatCall(call.callId)
                    }
                }.onFailure {
                    Timber.e(it)
                }
            }
        }

    }

    private fun monitorChatCall(callId: Long) {
        monitorChatCallJob?.cancel()
        monitorChatCallJob = viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.callId == callId }
                .catch {
                    Timber.e(it)
                }
                .collect { call ->
                    _state.update { it.copy(currentChatCall = call) }
                    call.changes?.apply {
                        if (contains(ChatCallChanges.Status)) {
                            Timber.d("Chat call status: ${call.status}")
                            when (call.status) {
                                ChatCallStatus.Destroyed -> {
                                    monitorChatCallJob?.cancel()
                                }

                                else -> {}
                            }
                        }
                    }
                }
        }
    }

    fun checkSensitiveItems(handles: List<Long>) = viewModelScope.launch {
        if (getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) {
            var sensitiveType = 0

            for (handle in handles) {
                val nodeId = NodeId(handle)
                val node = getNodeByIdUseCase(nodeId)

                if (node == null || node.isOutShare() || node !is FolderNode) continue

                if (node.isMarkedSensitive || node.isSensitiveInherited) {
                    sensitiveType = 1
                    break
                }

                if (hasSensitiveDescendantUseCase(nodeId)) {
                    sensitiveType = 2
                }
            }

            if (sensitiveType == 1 && handles.size == 1) sensitiveType = 1
            else if (sensitiveType == 1 && handles.size > 1) sensitiveType = 2
            else if (sensitiveType == 2 && handles.size == 1) sensitiveType = 3
            else if (sensitiveType == 2 && handles.size > 1) sensitiveType = 4

            _sensitiveItemsCount.value = sensitiveType
        } else {
            _sensitiveItemsCount.value = 0
        }
    }

    fun clearSensitiveItemsCheck() {
        _sensitiveItemsCount.value = null
    }
}
