package mega.privacy.android.feature.sharelink.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetPasswordStrengthUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.filelink.EncryptLinkWithPasswordUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import timber.log.Timber

/**
 * ViewModel for the revamped Link settings editor screen.
 *
 * Holds the editable security-option selection (separate key, expiry, password), tracks whether
 * it differs from the initial state so Save can enable, and on [onSave] applies the changes to the
 * node's public link via [ExportNodeUseCase] (expiry) and [EncryptLinkWithPasswordUseCase]
 * (password). The account type drives Pro gating of the expiry and password rows.
 */
@HiltViewModel(assistedFactory = LinkSettingsViewModel.Factory::class)
class LinkSettingsViewModel @AssistedInject constructor(
    @Assisted private val args: Args,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val exportNodeUseCase: ExportNodeUseCase,
    private val encryptLinkWithPasswordUseCase: EncryptLinkWithPasswordUseCase,
    private val getPasswordStrengthUseCase: GetPasswordStrengthUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinkSettingsUiState())
    val uiState: StateFlow<LinkSettingsUiState> = _uiState.asStateFlow()

    private var publicLink: String? = null

    init {
        loadNode()
        monitorAccountDetail()
    }

    fun onSeparateKeyEnabled(enabled: Boolean) =
        update { it.copy(isSeparateKeyEnabled = enabled) }

    fun onExpiryEnabled(enabled: Boolean) =
        update { it.copy(isExpiryEnabled = enabled, expiryDate = if (enabled) it.expiryDate else null) }

    fun onExpiryDateChanged(expiryDate: Long) =
        update { it.copy(expiryDate = expiryDate) }

    fun onPasswordEnabled(enabled: Boolean) = update {
        it.copy(
            isPasswordEnabled = enabled,
            password = if (enabled) it.password else null,
            passwordStrength = if (enabled) it.passwordStrength else null,
        )
    }

    fun onPasswordChanged(password: String) {
        update { it.copy(password = password) }
        viewModelScope.launch {
            val strength = password.takeIf(String::isNotEmpty)
                ?.let { runCatching { getPasswordStrengthUseCase(it) }.getOrNull() }
            update { it.copy(passwordStrength = strength) }
        }
    }

    fun onSave() {
        val handle = args.handles.firstOrNull()
        val current = _uiState.value
        if (current.isSaving || handle == null || !current.isDirty || !current.isValid) return

        update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { applyChanges(NodeId(handle), current) }
                .onSuccess { update { it.copy(isSaving = false, savedEvent = triggered) } }
                .onFailure { throwable ->
                    Timber.e(throwable, "Failed to save link settings")
                    update { it.copy(isSaving = false, errorEvent = triggered) }
                }
        }
    }

    fun onSavedEventConsumed() = update { it.copy(savedEvent = consumed) }

    fun onErrorEventConsumed() = update { it.copy(errorEvent = consumed) }

    private suspend fun applyChanges(nodeId: NodeId, state: LinkSettingsUiState) {
        if (state.isExpiryEnabled) {
            exportNodeUseCase(
                nodeToExport = nodeId,
                expireTime = state.expiryDate,
                callerName = CALLER_NAME,
            )
        }
        val password = state.password
        if (state.isPasswordEnabled && !password.isNullOrBlank()) {
            publicLink?.takeIf(String::isNotEmpty)?.let { link ->
                encryptLinkWithPasswordUseCase(link, password)
            }
        }
    }

    private fun loadNode() {
        val handle = args.handles.firstOrNull() ?: return
        viewModelScope.launch {
            publicLink = runCatching { getNodeByIdUseCase(NodeId(handle))?.exportedData?.publicLink }
                .onFailure { Timber.e(it, "Failed to load node for link settings") }
                .getOrNull()
        }
    }

    private fun monitorAccountDetail() {
        viewModelScope.launch {
            monitorAccountDetailUseCase()
                .catch { Timber.e(it) }
                .collect { accountDetail ->
                    update { it.copy(isLoading = false, accountType = accountDetail.levelDetail?.accountType) }
                }
        }
    }

    private fun update(transform: (LinkSettingsUiState) -> LinkSettingsUiState) =
        _uiState.update { transform(it).withComputedFlags() }

    private fun LinkSettingsUiState.withComputedFlags() =
        copy(hasUnsavedChanges = isDirty, isSaveEnabled = isDirty && isValid && !isSaving)

    private val LinkSettingsUiState.isDirty: Boolean
        get() = isSeparateKeyEnabled || isExpiryEnabled || isPasswordEnabled ||
                expiryDate != null || !password.isNullOrEmpty()

    private val LinkSettingsUiState.isValid: Boolean
        get() = when {
            isExpiryEnabled && expiryDate == null -> false
            isPasswordEnabled && password.isNullOrBlank() -> false
            else -> true
        }

    /**
     * Assisted factory arguments.
     *
     * @property handles Node handles whose link settings are being edited.
     */
    data class Args(val handles: List<Long>)

    @AssistedFactory
    interface Factory {
        fun create(args: Args): LinkSettingsViewModel
    }

    private companion object {
        const val CALLER_NAME = "LinkSettingsViewModel"
    }
}
