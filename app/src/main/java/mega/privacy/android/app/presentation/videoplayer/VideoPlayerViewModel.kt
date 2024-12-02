package mega.privacy.android.app.presentation.videoplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.videoplayer.mapper.VideoPlayerItemMapper
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerItem
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerUiState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.BACKUPS_ADAPTER
import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.Constants.FAVOURITES_ADAPTER
import mega.privacy.android.app.utils.Constants.FILE_BROWSER_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.FROM_MEDIA_DISCOVERY
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_SIZE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.LINKS_ADAPTER
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_BUCKET_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.node.backup.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetLocalLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.GetParentNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.GetUserNameByEmailUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodeByHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByEmailUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByHandlesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByParentHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromInSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromOutSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromPublicLinksUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideosByParentHandleFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideosBySearchTypeUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByIdUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for video player.
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @VideoPlayer private val mediaPlayerGateway: MediaPlayerGateway,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val videoPlayerItemMapper: VideoPlayerItemMapper,
    private val getVideoNodeByHandleUseCase: GetVideoNodeByHandleUseCase,
    private val getVideoNodesUseCase: GetVideoNodesUseCase,
    private val getVideoNodesFromPublicLinksUseCase: GetVideoNodesFromPublicLinksUseCase,
    private val getVideoNodesFromInSharesUseCase: GetVideoNodesFromInSharesUseCase,
    private val getVideoNodesFromOutSharesUseCase: GetVideoNodesFromOutSharesUseCase,
    private val getVideoNodesByEmailUseCase: GetVideoNodesByEmailUseCase,
    private val getUserNameByEmailUseCase: GetUserNameByEmailUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getVideosBySearchTypeUseCase: GetVideosBySearchTypeUseCase,
    private val getVideoNodesByParentHandleUseCase: GetVideoNodesByParentHandleUseCase,
    private val getVideoNodesByHandlesUseCase: GetVideoNodesByHandlesUseCase,
    private val getRootNodeFromMegaApiFolderUseCase: GetRootNodeFromMegaApiFolderUseCase,
    private val getParentNodeFromMegaApiFolderUseCase: GetParentNodeFromMegaApiFolderUseCase,
    private val getVideosByParentHandleFromMegaApiFolderUseCase: GetVideosByParentHandleFromMegaApiFolderUseCase,
    private val monitorSubFolderMediaDiscoverySettingsUseCase: MonitorSubFolderMediaDiscoverySettingsUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase,
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase,
    private val megaApiFolderHttpServerStopUseCase: MegaApiFolderHttpServerStopUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val megaApiHttpServerStop: MegaApiHttpServerStopUseCase,
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val getFileTypeInfoByNameUseCase: GetFileTypeInfoByNameUseCase,
    private val getOfflineNodeInformationByIdUseCase: GetOfflineNodeInformationByIdUseCase,
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase,
    private val getLocalLinkFromMegaApiUseCase: GetLocalLinkFromMegaApiUseCase,
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val getFileByPathUseCase: GetFileByPathUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    internal val uiState = _uiState.asStateFlow()

    private var needStopStreamingServer = false
    private var playerRetry = 0

    init {
        setupTransferListener()
    }

    /**
     * Setup transfer listener
     */
    private fun setupTransferListener() =
        viewModelScope.launch {
            monitorTransferEventsUseCase()
                .catch {
                    Timber.e(it)
                }.collect { event ->
                    if (event is TransferEvent.TransferTemporaryErrorEvent) {
                        val error = event.error
                        val transfer = event.transfer
                        if (transfer.nodeHandle == _uiState.value.currentPlayingHandle
                            && ((error is QuotaExceededMegaException
                                    && !transfer.isForeignOverQuota
                                    && error.value != 0L)
                                    || error is BlockedMegaException)
                        ) {
                            _uiState.update { it.copy(error = error) }
                        }
                    }
                }
        }

    internal fun initVideoPlaybackSources(intent: Intent?) {
        viewModelScope.launch {
            buildPlaybackSources(intent)
        }
    }

    private suspend fun buildPlaybackSources(intent: Intent?) {
        if (intent == null || !validateIntent(intent)) return

        val launchSource = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val uri = intent.data
        val currentPlayingHandle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
        val currentPlayingFileName = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME).orEmpty()
        needStopStreamingServer =
            intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)
        playerRetry = 0

        val currentPlayingUri = getCurrentPlayingUri(uri, launchSource, currentPlayingHandle)
        if (currentPlayingUri == null) {
            logInvalidParam("folder link uri is null")
            return
        }

        val currentPlayingMediaItem = MediaItem.Builder()
            .setUri(currentPlayingUri)
            .setMediaId(currentPlayingHandle.toString())
            .build()

        updateStateWithMediaItem(currentPlayingMediaItem, currentPlayingFileName)

        if (!intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)) {
            setPlayingItem(currentPlayingHandle, currentPlayingFileName, launchSource)
            return
        }

        if (launchSource != OFFLINE_ADAPTER && launchSource != ZIP_ADAPTER) {
            needStopStreamingServer = needStopStreamingServer || setupStreamingServer(launchSource)
        }

        withContext(ioDispatcher) {
            handlePlaybackSourceByLaunchSource(intent, launchSource, currentPlayingHandle)
        }
    }

    private fun validateIntent(intent: Intent): Boolean {
        val isValid = when {
            !intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true) -> {
                logInvalidParam("Rebuild playlist param is false")
                false
            }

            intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE) == INVALID_VALUE -> {
                logInvalidParam("Launch source is invalid")
                false
            }

            intent.data == null -> {
                logInvalidParam("URI is null")
                false
            }

            intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE) == INVALID_HANDLE -> {
                logInvalidParam("The first playing video handle is invalid")
                false
            }

            intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) == null -> {
                logInvalidParam("The first playing video file name is null")
                false
            }

            else -> true
        }
        return isValid
    }

    private fun logInvalidParam(message: String) {
        Timber.d("Build playback sources failed: $message")
        _uiState.update { it.copy(isRetry = false) }
    }

    private suspend fun setupStreamingServer(launchSource: Int): Boolean {
        val isServerRunning = if (launchSource == FOLDER_LINK_ADAPTER && !hasCredentialsUseCase()) {
            megaApiFolderHttpServerIsRunningUseCase()
        } else {
            megaApiHttpServerIsRunningUseCase()
        }

        if (isServerRunning != 0) return false

        if (launchSource == FOLDER_LINK_ADAPTER && !hasCredentialsUseCase()) {
            megaApiFolderHttpServerStartUseCase()
        } else {
            megaApiHttpServerStartUseCase()
        }

        return true
    }

    private suspend fun getCurrentPlayingUri(uri: Uri?, launchSource: Int, handle: Long) =
        when (launchSource) {
            FOLDER_LINK_ADAPTER -> {
                val url = if (hasCredentialsUseCase()) {
                    getLocalFolderLinkFromMegaApiUseCase(handle)
                } else {
                    getLocalFolderLinkFromMegaApiFolderUseCase(handle)
                }
                url?.let { Uri.parse(it) }
            }

            else -> uri
        }

    private fun updateStateWithMediaItem(mediaItem: MediaItem, fileName: String) {
        MediaPlaySources(
            mediaItems = listOf(mediaItem),
            newIndexForCurrentItem = INVALID_VALUE,
            nameToDisplay = fileName
        ).also { sources ->
            _uiState.update { it.copy(mediaPlaySources = sources) }
            buildPlaybackSourcesForPlayer(sources)
        }
    }

    private fun buildPlaybackSourcesForPlayer(mediaPlaySources: MediaPlaySources) {
        Timber.d("Playback sources: ${mediaPlaySources.mediaItems.size} items")
        with(mediaPlayerGateway) {
            buildPlaySources(mediaPlaySources)
            setPlayWhenReady(_uiState.value.isPaused && mediaPlaySources.isRestartPlaying)
            playerPrepare()
        }
        mediaPlaySources.nameToDisplay?.let { name ->
            _uiState.update { it.copy(metadata = Metadata(null, null, null, nodeName = name)) }
        }
    }

    private suspend fun setPlayingItem(handle: Long, fileName: String?, source: Int) {
        val node = getVideoNodeByHandleUseCase(handle)
        val thumbnail = getThumbnailForNode(node, handle, source)
        val playingItem = videoPlayerItemMapper(
            nodeHandle = handle,
            nodeName = fileName.orEmpty(),
            thumbnail = thumbnail,
            type = MediaQueueItemType.Playing,
            size = node?.size ?: INVALID_SIZE,
            duration = node?.duration ?: 0.seconds,
        )

        _uiState.update { it.copy(items = listOf(playingItem)) }
    }

    private suspend fun getThumbnailForNode(
        node: TypedVideoNode?,
        handle: Long,
        source: Int,
    ) = when {
        node == null -> null
        source == OFFLINE_ADAPTER -> getThumbnailUseCase(handle)
        else -> runCatching {
            File(
                ThumbnailUtils.getThumbFolder(context),
                node.base64Id.plus(FileUtil.JPG_EXTENSION)
            )
        }.getOrNull()
    }

    private suspend fun handlePlaybackSourceByLaunchSource(
        intent: Intent,
        launchSource: Int,
        playingHandle: Long,
    ) {
        when (launchSource) {
            OFFLINE_ADAPTER -> handleOfflineSource(intent, playingHandle)
            ZIP_ADAPTER -> handleZipSource(intent, playingHandle)
            else -> handleGeneralSource(intent, launchSource, playingHandle)
        }
    }

    private suspend fun handleOfflineSource(intent: Intent, playingHandle: Long) {
        val parentId = intent.getIntExtra(INTENT_EXTRA_KEY_PARENT_ID, -1)
        val title = if (parentId == -1) {
            context.getString(R.string.section_saved_for_offline_new)
        } else {
            runCatching {
                getOfflineNodeInformationByIdUseCase(parentId)
            }.getOrNull()?.name.orEmpty()
        }
        buildPlaybackSourcesByOfflineNodes(title, parentId, playingHandle)
    }

    private suspend fun buildPlaybackSourcesByOfflineNodes(
        title: String,
        parentId: Int,
        firstPlayHandle: Long,
    ) {
        runCatching {
            getOfflineNodesByParentIdUseCase(parentId)
        }.onSuccess { list ->
            val mediaItems = mutableListOf<MediaItem>()
            var currentPlayingIndex = -1
            val videoPlayerItems = list.filter {
                it.fileTypeInfo is VideoFileTypeInfo && it.fileTypeInfo?.isSupported == true
            }.mapIndexed { index, item ->
                if (item.handle.toLong() == firstPlayHandle) currentPlayingIndex = index

                runCatching { Uri.parse(item.absolutePath) }.getOrNull()?.let {
                    mediaItems.add(
                        MediaItem.Builder()
                            .setUri(it)
                            .setMediaId(item.handle)
                            .build()
                    )
                }

                val thumbnailFile = runCatching {
                    item.thumbnail?.let { File(it) }
                }.getOrNull()

                videoPlayerItemMapper(
                    nodeHandle = item.handle.toLong(),
                    nodeName = item.name,
                    thumbnail = thumbnailFile,
                    type = getMediaQueueItemType(index, currentPlayingIndex),
                    size = item.totalSize,
                    duration = (item.fileTypeInfo as? VideoFileTypeInfo)?.duration ?: 0.seconds,
                )
            }

            updatePlaybackSources(
                videoPlayerItems = videoPlayerItems,
                mediaItems = mediaItems,
                title = title,
                currentPlayingIndex = currentPlayingIndex,
                firstPlayHandle = firstPlayHandle
            )
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun handleZipSource(intent: Intent, playingHandle: Long) {
        intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)?.let { zipPath ->
            buildPlaybackSourcesByFiles(zipPath, playingHandle)
        }
    }

    private fun getMediaQueueItemType(currentIndex: Int, playingIndex: Int) =
        when {
            currentIndex == playingIndex -> MediaQueueItemType.Playing
            playingIndex == -1 || currentIndex < playingIndex -> MediaQueueItemType.Previous
            else -> MediaQueueItemType.Next
        }

    private fun updatePlaybackSources(
        videoPlayerItems: List<VideoPlayerItem>,
        mediaItems: List<MediaItem>,
        title: String,
        currentPlayingIndex: Int,
        firstPlayHandle: Long,
    ) {
        val mediaPlaySources = MediaPlaySources(
            mediaItems = mediaItems,
            newIndexForCurrentItem = currentPlayingIndex,
            nameToDisplay = null
        )

        _uiState.update {
            it.copy(
                items = videoPlayerItems,
                mediaPlaySources = mediaPlaySources,
                playQueueTitle = title,
                currentPlayingIndex = currentPlayingIndex,
                currentPlayingHandle = firstPlayHandle
            )
        }
        buildPlaybackSourcesForPlayer(mediaPlaySources)
    }

    private suspend fun buildPlaybackSourcesByFiles(zipPath: String, firstPlayHandle: Long) {
        runCatching {
            val (title, files) = getFileByPathUseCase(zipPath)?.parentFile.let { parentFile ->
                parentFile?.name.orEmpty() to parentFile?.listFiles().orEmpty()
            }
            val mediaItems = mutableListOf<MediaItem>()
            var currentPlayingIndex = -1
            val videoPlayerItems = files.filter {
                it.isFile && getFileTypeInfoByNameUseCase(it.name) is VideoFileTypeInfo
            }.mapIndexed { index, file ->
                if (file.name.hashCode().toLong() == firstPlayHandle) currentPlayingIndex = index

                mediaItems.add(
                    MediaItem.Builder()
                        .setUri(FileUtil.getUriForFile(context, file))
                        .setMediaId(file.name.hashCode().toString())
                        .build()
                )

                videoPlayerItemMapper(
                    nodeHandle = file.name.hashCode().toLong(),
                    nodeName = file.name,
                    thumbnail = null,
                    type = getMediaQueueItemType(index, currentPlayingIndex),
                    size = file.length(),
                    duration = 0.seconds,
                )
            }

            updatePlaybackSources(
                videoPlayerItems = videoPlayerItems,
                mediaItems = mediaItems,
                title = title,
                currentPlayingIndex = currentPlayingIndex,
                firstPlayHandle = firstPlayHandle
            )
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun handleGeneralSource(
        intent: Intent,
        launchSource: Int,
        playingHandle: Long,
    ) {
        val parentHandle = intent.getLongExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
        val order = getSortOrderFromIntent(intent)
        val (title, videoNodes) = when (launchSource) {
            VIDEO_BROWSE_ADAPTER ->
                context.getString(R.string.sortby_type_video_first) to getVideoNodesUseCase(order)

            RECENTS_ADAPTER, RECENTS_BUCKET_ADAPTER -> {
                val videoNodes = intent.getLongArrayExtra(NODE_HANDLES)?.let { handles ->
                    getVideoNodesByHandlesUseCase(handles.toList())
                }.orEmpty()
                context.getString(R.string.section_recents) to videoNodes
            }

            FOLDER_LINK_ADAPTER -> {
                val parentNode = if (parentHandle == INVALID_HANDLE) {
                    getRootNodeFromMegaApiFolderUseCase()
                } else {
                    getParentNodeFromMegaApiFolderUseCase(parentHandle)
                }

                val videoNodes = parentNode?.let {
                    getVideosByParentHandleFromMegaApiFolderUseCase(
                        parentHandle = it.id.longValue,
                        order = order
                    )
                }.orEmpty()

                (parentNode?.name.orEmpty()) to videoNodes
            }

            SEARCH_BY_ADAPTER -> {
                val title = intent.getStringExtra(INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE).orEmpty()
                val videoNodes = intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)
                    ?.let { handles ->
                        getVideoNodesByHandlesUseCase(handles.toList())
                    }.orEmpty()
                title to videoNodes
            }

            FILE_BROWSER_ADAPTER,
            RUBBISH_BIN_ADAPTER,
            BACKUPS_ADAPTER,
            LINKS_ADAPTER,
            INCOMING_SHARES_ADAPTER,
            OUTGOING_SHARES_ADAPTER,
            CONTACT_FILE_ADAPTER,
            FROM_MEDIA_DISCOVERY,
            FROM_IMAGE_VIEWER,
            FROM_ALBUM_SHARING,
            FAVOURITES_ADAPTER,
                -> {
                when {
                    launchSource == LINKS_ADAPTER && parentHandle == INVALID_HANDLE ->
                        context.getString(R.string.tab_links_shares) to getVideoNodesFromPublicLinksUseCase(
                            order
                        )

                    launchSource == INCOMING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE ->
                        context.getString(R.string.tab_incoming_shares) to getVideoNodesFromInSharesUseCase(
                            order
                        )

                    launchSource == OUTGOING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE ->
                        context.getString(R.string.tab_outgoing_shares) to getVideoNodesFromOutSharesUseCase(
                            lastHandle = INVALID_HANDLE,
                            order = order
                        )

                    launchSource == CONTACT_FILE_ADAPTER && parentHandle == INVALID_HANDLE -> {
                        intent.getStringExtra(Constants.INTENT_EXTRA_KEY_CONTACT_EMAIL)
                            ?.let { email ->
                                val videoNodes = getVideoNodesByEmailUseCase(email).orEmpty()
                                val userName = getUserNameByEmailUseCase(email)
                                val title = if (userName == null) {
                                    ""
                                } else {
                                    "${context.getString(R.string.title_incoming_shares_with_explorer)} $userName"
                                }
                                title to videoNodes
                            } ?: ("" to emptyList())
                    }

                    else -> {
                        val parentNode =
                            if (parentHandle == INVALID_HANDLE) {
                                when (launchSource) {
                                    RUBBISH_BIN_ADAPTER -> getRubbishNodeUseCase()
                                    BACKUPS_ADAPTER -> getBackupsNodeUseCase()
                                    else -> getRootNodeUseCase()
                                }
                            } else {
                                getVideoNodeByHandleUseCase(parentHandle)
                            }
                        val title =
                            if (parentHandle == INVALID_HANDLE) {
                                context.getString(
                                    when (launchSource) {
                                        RUBBISH_BIN_ADAPTER -> R.string.section_rubbish_bin
                                        BACKUPS_ADAPTER -> R.string.home_side_menu_backups_title
                                        else -> R.string.section_cloud_drive
                                    }
                                )
                            } else {
                                parentNode?.name
                            }.orEmpty()

                        val videoNodes = parentNode?.let {
                            if (launchSource == FROM_MEDIA_DISCOVERY) {
                                getVideosBySearchTypeUseCase(
                                    handle = it.id.longValue,
                                    recursive = monitorSubFolderMediaDiscoverySettingsUseCase().first(),
                                    order = order
                                )
                            } else {
                                getVideoNodesByParentHandleUseCase(
                                    parentHandle = it.id.longValue,
                                    order = order
                                )
                            }
                        }.orEmpty()

                        title to videoNodes
                    }
                }
            }

            else -> {
                "" to emptyList()
            }
        }
        buildPlaybackSourcesByNodes(title, videoNodes, playingHandle, launchSource)
    }

    private fun getSortOrderFromIntent(intent: Intent): SortOrder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(
                INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                SortOrder::class.java
            ) ?: SortOrder.ORDER_DEFAULT_ASC
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN) as SortOrder?
                ?: SortOrder.ORDER_DEFAULT_ASC
        }

    private suspend fun buildPlaybackSourcesByNodes(
        title: String,
        videoNodes: List<TypedVideoNode>,
        firstPlayHandle: Long,
        launchSource: Int,
    ) {
        val mediaItems = mutableListOf<MediaItem>()
        var currentPlayingIndex = -1
        val videoPlayerItems = videoNodes.mapIndexed { index, node ->
            runCatching {
                if (node.id.longValue == firstPlayHandle) currentPlayingIndex = index

                getMediaItemForNode(node, launchSource)?.let { mediaItems.add(it) }

                videoPlayerItemMapper(
                    nodeHandle = node.id.longValue,
                    nodeName = node.name,
                    thumbnail = node.thumbnailPath?.let { path ->
                        File(path)
                    },
                    type = getMediaQueueItemType(index, currentPlayingIndex),
                    size = node.size,
                    duration = node.duration,
                )
            }.onFailure {
                Timber.e(it)
            }.getOrNull()
        }.filterNotNull()

        updatePlaybackSources(
            videoPlayerItems = videoPlayerItems,
            mediaItems = mediaItems,
            title = title,
            currentPlayingIndex = currentPlayingIndex,
            firstPlayHandle = firstPlayHandle
        )
    }

    private suspend fun getMediaItemForNode(node: TypedVideoNode, launchSource: Int) =
        getLocalFilePathUseCase(node).let { localPath ->
            if (localPath != null && isLocalFile(node, localPath)) {
                MediaItem.Builder()
                    .setUri(FileUtil.getUriForFile(context, File(localPath)))
                    .setMediaId(node.id.longValue.toString())
                    .build()
            } else {
                when (launchSource) {
                    FOLDER_LINK_ADAPTER -> {
                        if (!hasCredentialsUseCase()) {
                            getLocalFolderLinkFromMegaApiFolderUseCase(node.id.longValue)
                        } else {
                            getLocalFolderLinkFromMegaApiUseCase(node.id.longValue)
                        }
                    }

                    else -> getLocalLinkFromMegaApiUseCase(node.id.longValue)
                }?.let { url ->
                    MediaItem.Builder()
                        .setUri(Uri.parse(url))
                        .setMediaId(node.id.longValue.toString())
                        .build()
                }
            }
        }

    private suspend fun isLocalFile(node: TypedFileNode, localPath: String): Boolean {
        val isFingerPrintAvailable = node.fingerprint?.let {
            it == getFingerprintUseCase(localPath)
        } ?: false
        return isOnMegaDownloads(node) || isFingerPrintAvailable
    }

    private fun isOnMegaDownloads(node: TypedFileNode): Boolean =
        File(FileUtil.getDownloadLocation(), node.name).let { file ->
            FileUtil.isFileAvailable(file) && file.length() == node.size
        }

    /**
     * onCleared
     */
    override fun onCleared() {
        super.onCleared()
        clear()
    }

    /**
     * Clear the state and flying task of this class, should be called in onDestroy.
     */
    private fun clear() {
        applicationScope.launch {
            if (needStopStreamingServer) {
                megaApiHttpServerStop()
                megaApiFolderHttpServerStopUseCase()
            }
        }
    }
}