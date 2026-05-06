package mega.privacy.android.feature.clouddrive.presentation.filelink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailUriRequest
import mega.privacy.android.domain.entity.toDuration
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.viewedlinks.SaveViewedLinkUseCase
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkAction
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkUiState
import mega.privacy.android.shared.nodes.extension.getIcon
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import timber.log.Timber
import java.io.File

@HiltViewModel(assistedFactory = FileLinkViewModel.Factory::class)
internal class FileLinkViewModel @AssistedInject constructor(
    private val getPublicNodeUseCase: GetPublicNodeUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val saveViewedLinkUseCase: SaveViewedLinkUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    @Assisted private val args: Args,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileLinkUiState(url = args.uriString))
    val uiState: StateFlow<FileLinkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            checkCredentials()
            args.uriString?.let { fetchPublicNode(it) }
        }
    }

    fun processAction(action: FileLinkAction) {
        when (action) {
            is FileLinkAction.DecryptionKeyEntered -> onDecryptionKeyEntered(action.key)
            FileLinkAction.DecryptionKeyDialogDismissed -> onDecryptionKeyDialogDismissed()
        }
    }

    private suspend fun checkCredentials() {
        val hasCredentials = runCatching { hasCredentialsUseCase() }
            .onFailure { Timber.e(it) }
            .getOrDefault(false)
        _uiState.update { it.copy(hasCredentials = hasCredentials) }
    }

    private fun fetchPublicNode(url: String, decryptionIntroduced: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(contentState = FileLinkContentState.Loading) }
            runCatching { getPublicNodeUseCase(url) }
                .onSuccess { node ->
                    val iconRes = node.getIcon(
                        originShares = false,
                        fileTypeIconMapper = fileTypeIconMapper,
                    )
                    val formattedDuration = node.type.toDuration()?.let { duration ->
                        durationInSecondsTextMapper(duration)
                    }
                    val thumbnailData = node.previewPath?.let { path ->
                        runCatching { ThumbnailUriRequest(UriPath.fromFile(File(path))) }.getOrNull()
                    }
                    _uiState.update {
                        it.copy(
                            contentState = FileLinkContentState.Loaded(
                                iconRes = iconRes,
                                thumbnailData = thumbnailData,
                                formattedDuration = formattedDuration,
                            ),
                            fileNode = node,
                        )
                    }
                    saveViewedFileLink(url, node)
                }
                .onFailure { error ->
                    handleFetchError(error, url, decryptionIntroduced)
                }
        }
    }

    private fun handleFetchError(
        error: Throwable,
        url: String,
        decryptionIntroduced: Boolean,
    ) {
        Timber.d("getPublicNode failed: $error")
        val nextState = when (error) {
            is PublicNodeException.DecryptionKeyRequired ->
                FileLinkContentState.DecryptionKeyRequired(url = url)

            is PublicNodeException.InvalidDecryptionKey ->
                if (decryptionIntroduced) {
                    FileLinkContentState.DecryptionKeyRequired(url = url, isKeyIncorrect = true)
                } else {
                    FileLinkContentState.Unavailable
                }

            is PublicNodeException.Expired -> FileLinkContentState.Expired
            is PublicNodeException -> FileLinkContentState.Unavailable
            else -> FileLinkContentState.Unavailable
        }
        _uiState.update { it.copy(contentState = nextState) }
    }

    private fun onDecryptionKeyEntered(key: String) {
        val currentState = _uiState.value.contentState
        if (currentState !is FileLinkContentState.DecryptionKeyRequired) return
        val trimmedKey = key.trim()
        if (trimmedKey.isEmpty()) return
        val url = currentState.url
        val urlWithKey = when {
            url.contains("#!") ->
                if (trimmedKey.startsWith("!")) "$url$trimmedKey" else "$url!$trimmedKey"

            else ->
                if (trimmedKey.startsWith("#")) "$url$trimmedKey" else "$url#$trimmedKey"
        }
        fetchPublicNode(urlWithKey, decryptionIntroduced = true)
    }

    private fun onDecryptionKeyDialogDismissed() {
        _uiState.update { it.copy(contentState = FileLinkContentState.Loading) }
    }

    private fun saveViewedFileLink(link: String, node: TypedFileNode) {
        viewModelScope.launch {
            val isEnabled = runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.ViewedLinks)
            }.getOrDefault(false)
            if (!isEnabled) return@launch
            runCatching {
                saveViewedLinkUseCase(
                    ViewedLink(
                        nodeHandle = node.id.longValue,
                        name = node.name,
                        linkUrl = link,
                        type = RecentlyUsedType.FileLink,
                        accessedTimestamp = null,
                    )
                )
            }.onFailure { Timber.e(it) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): FileLinkViewModel
    }

    data class Args(
        val uriString: String?,
    )
}
