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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetPasswordStrengthUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.filelink.EncryptLinkWithPasswordUseCase
import mega.privacy.android.domain.usecase.link.SplitLinkAndKeyUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.feature.sharelink.session.LinkPassword
import mega.privacy.android.feature.sharelink.session.ShareLinkPasswordCache
import mega.privacy.android.feature.sharelink.session.ShareLinkPublicLinkCache
import mega.privacy.android.feature.sharelink.session.ShareLinkSeparateKeyCache
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
    private val splitLinkAndKeyUseCase: SplitLinkAndKeyUseCase,
    private val passwordCache: ShareLinkPasswordCache,
    private val separateKeyCache: ShareLinkSeparateKeyCache,
    private val publicLinkCache: ShareLinkPublicLinkCache,
) : ViewModel() {

    private val handle: Long? = args.subject.cacheKey
    private val isAlbum: Boolean = args.subject is ShareLinkSubject.Album
    private val cachedPassword: LinkPassword? =
        handle?.takeUnless { isAlbum }?.let(passwordCache::get)
    private val cachedSeparateKey: Boolean = handle?.let(separateKeyCache::get) ?: false

    private val _uiState = MutableStateFlow(
        LinkSettingsUiState(
            isAlbum = isAlbum,
            isSeparateKeyEnabled = cachedSeparateKey,
            initialSeparateKeyEnabled = cachedSeparateKey,
            isPasswordEnabled = cachedPassword != null,
            isPasswordAlreadySet = cachedPassword != null,
            initialPassword = cachedPassword?.password,
            password = cachedPassword?.password,
        )
    )
    val uiState: StateFlow<LinkSettingsUiState> = _uiState.asStateFlow()

    /**
     * The link being edited, seeded from what the Share link screen resolved. An album is not a
     * node, so [loadNode] can never find one for it; a node overwrites this with its own once read.
     */
    private var publicLink: String? = handle?.let(publicLinkCache::get)

    init {
        loadLinkSettings()
        cachedPassword?.password?.let(::computeStrength)
    }

    /**
     * Separate key and password protection are mutually exclusive: a password-protected link
     * already encrypts the key, so enabling one clears the other. At most one of
     * [LinkSettingsUiState.isSeparateKeyEnabled] and [LinkSettingsUiState.isPasswordEnabled] is
     * ever true.
     */
    fun onSeparateKeyEnabled(enabled: Boolean) = update {
        if (enabled) {
            it.copy(
                isSeparateKeyEnabled = true,
                isPasswordEnabled = false,
                password = null,
                passwordStrength = null,
            )
        } else {
            it.copy(isSeparateKeyEnabled = false)
        }
    }

    /**
     * Re-enabling the expiry restores the date the link already had. Without that, toggling off and
     * back on dropped the date silently and left the row on with an empty field and Save disabled,
     * with nothing on screen explaining why.
     */
    fun onExpiryEnabled(enabled: Boolean) = updateUnlessAlbum {
        if (enabled) {
            it.copy(isExpiryEnabled = true, expiryDate = it.expiryDate ?: it.initialExpiryDate)
        } else {
            it.copy(isExpiryEnabled = false, expiryDate = null)
        }
    }

    fun onExpiryDateChanged(expiryDate: Long) = updateUnlessAlbum {
        it.copy(expiryDate = expiryDate)
    }

    /** @see onSeparateKeyEnabled for the invariant this upholds from the other side. */
    fun onPasswordEnabled(enabled: Boolean) = updateUnlessAlbum {
        if (enabled) {
            it.copy(
                isSeparateKeyEnabled = false,
                isPasswordEnabled = true,
            )
        } else {
            it.copy(
                isPasswordEnabled = false,
                password = null,
                passwordStrength = null,
            )
        }
    }

    fun onPasswordChanged(password: String) {
        if (isAlbum) return
        update { it.copy(password = password) }
        computeStrength(password)
    }

    private fun computeStrength(password: String) {
        viewModelScope.launch {
            val strength = password.takeIf(String::isNotEmpty)
                ?.let { runCatching { getPasswordStrengthUseCase(it) }.getOrNull() }
            update { it.copy(passwordStrength = strength) }
        }
    }

    fun onSave() {
        val handle = handle ?: return
        val current = _uiState.value
        if (current.isSaving || !current.isDirty || !current.isValid) return

        update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { applyChanges(handle, current) }
                .onSuccess { savedLink ->
                    update {
                        it.copy(isSaving = false, savedLink = savedLink, savedEvent = triggered)
                    }
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "Failed to save link settings")
                    update { it.copy(isSaving = false, errorEvent = triggered) }
                }
        }
    }

    fun onSavedEventConsumed() = update { it.copy(savedEvent = consumed, savedLink = null) }

    fun onErrorEventConsumed() = update { it.copy(errorEvent = consumed) }

    /**
     * Applies the pending changes, writing any password change/removal to the shared
     * [ShareLinkPasswordCache] so the Share link screen reflects it.
     *
     * @return The link as it stands once the changes are applied, for the Share link screen to put
     * back on the clipboard, or null when no link is known (an album).
     */
    private suspend fun applyChanges(
        handle: Long,
        state: LinkSettingsUiState,
    ): String? {
        val link = publicLink?.takeIf(String::isNotEmpty)
        val password = state.password
            ?.takeUnless(String::isBlank)
            ?.takeIf { state.isPasswordEnabled }

        // Everything that can fail runs before anything is committed, so a failure leaves the link
        // as it was and the error the screen reports is true. Encrypting only computes a string
        // from the link, and the expiry export is the sole remote side effect; the cache writes
        // below cannot fail. Committing first meant a later failure left the Share link screen
        // showing changes the user had just been told did not apply.
        val encryptedLink = if (password != null && link != null) {
            encryptLinkWithPasswordUseCase(link, password).takeIf(String::isNotEmpty)
        } else {
            null
        }

        if (state.isExpiryDirty) {
            val expireTimeSeconds = state.expiryDate
                ?.takeIf { state.isExpiryEnabled }
                ?.milliseconds?.inWholeSeconds
            exportNodeUseCase(
                nodeToExport = NodeId(handle),
                expireTime = expireTimeSeconds,
                callerName = CALLER_NAME,
            )
        }

        if (state.isSeparateKeyDirty) {
            separateKeyCache.set(handle, state.isSeparateKeyEnabled)
        }
        // The cache write and the choice of link to copy are settled separately. Deciding both in
        // one `when` let removing a password return the full link and shadow a separate-key change
        // made in the same save, putting the decryption key on the clipboard.
        if (password != null) {
            passwordCache.set(
                handle,
                LinkPassword(password = password, linkWithPassword = encryptedLink),
            )
        } else if (state.isPasswordAlreadySet) {
            passwordCache.set(handle, null)
        }

        // Mirrors the Share link screen's own rule for the link it shows: password-encrypted if
        // protected, otherwise the key-less half when the key is sent separately.
        return when {
            password != null -> encryptedLink ?: link
            state.isSeparateKeyEnabled ->
                link?.let { splitLinkAndKeyUseCase(it).linkWithoutKey ?: it }

            else -> link
        }
    }

    /**
     * Loads everything the first frame depends on and publishes it together.
     *
     * The node and the account type used to be collected by two independent coroutines, and
     * whichever finished first cleared [LinkSettingsUiState.isLoading]. In practice that was always
     * the account detail — it serves a cached value while the node read goes to the SDK — so the
     * content composed with the expiry toggle still off and the node load flipped it a moment
     * later, animating the toggle after the screen was already visible. Combining them means the
     * screen only ever renders once both are known.
     */
    private fun loadLinkSettings() {
        viewModelScope.launch {
            combine(
                flow { emit(loadNode()) },
                monitorAccountDetailUseCase()
                    .map { it.levelDetail?.accountType }
                    .catch { throwable ->
                        // Without an account type the skeleton would never clear, so fall back to
                        // null: the Pro rows simply stay locked.
                        Timber.e(throwable, "Failed to monitor the account detail")
                        emit(null)
                    },
                ::Pair,
            ).collect { (node, accountType) ->
                update { state ->
                    // The node is read once, but the account type keeps arriving. Only the first
                    // emission seeds from the node, so a later account change cannot overwrite an
                    // edit the user has already made.
                    val seeded = if (state.isLoading) state.seededFrom(node) else state
                    seeded.copy(isLoading = false, accountType = accountType)
                }
            }
        }
    }

    /** Reads the node behind the link, or null for an album, which is not a node. */
    private suspend fun loadNode(): TypedNode? {
        val handle = handle?.takeUnless { isAlbum } ?: return null
        val node = runCatching { getNodeByIdUseCase(NodeId(handle)) }
            .onFailure { Timber.e(it, "Failed to load node for link settings") }
            .getOrNull()
        // Keeps the seeded link when the node cannot be read, so a transient failure does not
        // turn the next save silent.
        node?.exportedData?.publicLink?.let { publicLink = it }
        return node
    }

    /** Applies the link's existing options, so the screen's first frame already reflects them. */
    private fun LinkSettingsUiState.seededFrom(node: TypedNode?): LinkSettingsUiState {
        val expiryMillis = node?.exportedData?.expirationTime?.seconds?.inWholeMilliseconds
        return if (expiryMillis == null) {
            copy(isFolder = node is FolderNode)
        } else {
            copy(
                isFolder = node is FolderNode,
                isExpiryEnabled = true,
                isExpiryAlreadySet = true,
                initialExpiryDate = expiryMillis,
                expiryDate = expiryMillis,
            )
        }
    }

    private fun update(transform: (LinkSettingsUiState) -> LinkSettingsUiState) =
        _uiState.update { transform(it).withComputedFlags() }

    /**
     * Applies [transform] only for a node subject.
     *
     * An album link supports neither an expiry nor a password, so the state must never be able to
     * describe one — the rows are absent from the screen, but the invariant belongs here rather
     * than resting on the UI. Ignoring the change keeps [LinkSettingsUiState.isSaveEnabled] false
     * too, so Save can never offer to apply something that would be dropped.
     */
    private fun updateUnlessAlbum(transform: (LinkSettingsUiState) -> LinkSettingsUiState) {
        if (isAlbum) return
        update(transform)
    }

    private fun LinkSettingsUiState.withComputedFlags() =
        copy(hasUnsavedChanges = isDirty, isSaveEnabled = isDirty && isValid && !isSaving)

    private val LinkSettingsUiState.isDirty: Boolean
        get() = isSeparateKeyDirty || isExpiryDirty || isPasswordDirty

    private val LinkSettingsUiState.isSeparateKeyDirty: Boolean
        get() = isSeparateKeyEnabled != initialSeparateKeyEnabled

    private val LinkSettingsUiState.isExpiryDirty: Boolean
        get() = if (isExpiryAlreadySet) {
            !isExpiryEnabled || expiryDate != initialExpiryDate
        } else {
            isExpiryEnabled || expiryDate != null
        }

    private val LinkSettingsUiState.isPasswordDirty: Boolean
        get() = if (isPasswordAlreadySet) {
            !isPasswordEnabled || password != initialPassword
        } else {
            isPasswordEnabled || !password.isNullOrEmpty()
        }

    private val LinkSettingsUiState.isValid: Boolean
        get() = when {
            isExpiryEnabled && expiryDate == null -> false
            isPasswordEnabled && password.isNullOrBlank() -> false
            else -> true
        }

    /**
     * Assisted factory arguments.
     *
     * @property subject Whose link settings are being edited: node handles or an album.
     */
    data class Args(val subject: ShareLinkSubject)

    @AssistedFactory
    interface Factory {
        fun create(args: Args): LinkSettingsViewModel
    }

    private companion object {
        const val CALLER_NAME = "LinkSettingsViewModel"
    }
}
