package mega.privacy.android.app.mediaplayer.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.gateway.AudioPlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.mapper.PlaylistItemMapper
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.playlist.PlaylistAdapter.Companion.TYPE_NEXT
import mega.privacy.android.app.mediaplayer.playlist.PlaylistAdapter.Companion.TYPE_PLAYING
import mega.privacy.android.app.mediaplayer.playlist.PlaylistAdapter.Companion.TYPE_PREVIOUS
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.playlist.finalizeItem
import mega.privacy.android.app.mediaplayer.playlist.updateNodeName
import mega.privacy.android.app.presentation.extensions.parcelableArrayList
import mega.privacy.android.app.search.callback.SearchCallback
import mega.privacy.android.app.utils.Constants.AUDIO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.BACKUPS_ADAPTER
import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.Constants.FILE_BROWSER_ADAPTER
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ARRAY_OFFLINE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_EMAIL
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FROM_DOWNLOAD_SERVICE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_SIZE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.LINKS_ADAPTER
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.PHOTO_SYNC_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_BUCKET_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.FileUtil.getDownloadLocation
import mega.privacy.android.app.utils.FileUtil.getUriForFile
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.MegaNodeUtil.isInRootLinksLevel
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFolderName
import mega.privacy.android.app.utils.ThumbnailUtils.getThumbFolder
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.AreCredentialsNullUseCase
import mega.privacy.android.domain.usecase.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetLocalLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetParentNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.GetThumbnailFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetThumbnailFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetUserNameByEmailUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodeByHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesByEmailUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesByHandlesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesByParentHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesFromInSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesFromOutSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesFromPublicLinksUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudiosByParentHandleFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioBackgroundPlayEnabledUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioShuffleEnabledUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioBackgroundPlayEnabledUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioShuffleEnabledUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaCancelToken
import timber.log.Timber
import java.io.File
import java.util.Collections
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * A class containing audio player service logic, because using ViewModel in Service
 * is not the standard scenario, so this class is actually not a subclass of ViewModel.
 */
class AudioPlayerServiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineThumbnailFileWrapper: GetOfflineThumbnailFileWrapper,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    @ApplicationScope private val sharingScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val playlistItemMapper: PlaylistItemMapper,
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase,
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase,
    private val megaApiFolderHttpServerStopUseCase: MegaApiFolderHttpServerStopUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val megaApiHttpServerStop: MegaApiHttpServerStopUseCase,
    private val areCredentialsNullUseCase: AreCredentialsNullUseCase,
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase,
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val getLocalLinkFromMegaApiUseCase: GetLocalLinkFromMegaApiUseCase,
    private val getThumbnailFromMegaApiUseCase: GetThumbnailFromMegaApiUseCase,
    private val getThumbnailFromMegaApiFolderUseCase: GetThumbnailFromMegaApiFolderUseCase,
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase,
    private val getParentNodeFromMegaApiFolderUseCase: GetParentNodeFromMegaApiFolderUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getRootNodeFromMegaApiFolderUseCase: GetRootNodeFromMegaApiFolderUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val getAudioNodeByHandleUseCase: GetAudioNodeByHandleUseCase,
    private val getAudioNodesFromPublicLinksUseCase: GetAudioNodesFromPublicLinksUseCase,
    private val getAudioNodesFromInSharesUseCase: GetAudioNodesFromInSharesUseCase,
    private val getAudioNodesFromOutSharesUseCase: GetAudioNodesFromOutSharesUseCase,
    private val getAudioNodesUseCase: GetAudioNodesUseCase,
    private val getAudioNodesByEmailUseCase: GetAudioNodesByEmailUseCase,
    private val getUserNameByEmailUseCase: GetUserNameByEmailUseCase,
    private val getAudiosByParentHandleFromMegaApiFolderUseCase: GetAudiosByParentHandleFromMegaApiFolderUseCase,
    private val getAudioNodesByParentHandleUseCase: GetAudioNodesByParentHandleUseCase,
    private val getAudioNodesByHandlesUseCase: GetAudioNodesByHandlesUseCase,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val setAudioBackgroundPlayEnabledUseCase: SetAudioBackgroundPlayEnabledUseCase,
    private val setAudioShuffleEnabledUseCase: SetAudioShuffleEnabledUseCase,
    private val setAudioRepeatModeUseCase: SetAudioRepeatModeUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    monitorAudioBackgroundPlayEnabledUseCase: MonitorAudioBackgroundPlayEnabledUseCase,
    monitorAudioShuffleEnabledUseCase: MonitorAudioShuffleEnabledUseCase,
    monitorAudioRepeatModeUseCase: MonitorAudioRepeatModeUseCase,
) : AudioPlayerServiceViewModelGateway, ExposedShuffleOrder.ShuffleChangeListener,
    SearchCallback.Data {
    private val compositeDisposable = CompositeDisposable()

    private var backgroundPlayEnabled = monitorAudioBackgroundPlayEnabledUseCase().stateIn(
        sharingScope,
        SharingStarted.Eagerly,
        true
    )

    private var shuffleEnabled = monitorAudioShuffleEnabledUseCase().stateIn(
        sharingScope,
        SharingStarted.Eagerly,
        false
    )

    private var audioRepeatToggleMode = monitorAudioRepeatModeUseCase().stateIn(
        sharingScope,
        SharingStarted.Eagerly,
        RepeatToggleMode.REPEAT_NONE
    )

    private var currentIntent: Intent? = null

    private val playerSource = MutableLiveData<MediaPlaySources>()

    private val mediaItemToRemove = MutableSharedFlow<Int>()

    private val nodeNameUpdate = MutableLiveData<String>()

    private val playingThumbnail = MutableLiveData<File>()

    private val playlistItemsFlow =
        MutableStateFlow<Pair<List<PlaylistItem>, Int>>(Pair(emptyList(), 0))

    private val playlistTitle = MutableLiveData<String>()

    private val retry = MutableLiveData<Boolean>()

    private val error = MutableLiveData<MegaException?>()

    private val itemsClearedState = MutableStateFlow<Boolean?>(null)

    private var actionMode = MutableLiveData<Boolean>()

    private val itemsSelectedCount = MutableLiveData<Int>()

    private var mediaPlayback = MutableLiveData<Boolean>()

    private val playlistItems = mutableListOf<PlaylistItem>()

    private val itemsSelectedMap = mutableMapOf<Long, PlaylistItem>()

    private var playlistSearchQuery: String? = null

    private var shuffleOrder: ShuffleOrder = ExposedShuffleOrder(0, this)

    private var playingHandle = INVALID_HANDLE

    private var paused = false

    private var playerRetry = 0

    private var needStopStreamingServer = false

    private var playSourceChanged: MutableList<MediaItem> = mutableListOf()
    private var playlistItemsChanged: MutableList<PlaylistItem> = mutableListOf()
    private var playingPosition = 0

    private var cancelToken: MegaCancelToken? = null

    private val cancellableJobs = mutableMapOf<String, Job>()

    private var isSearchMode: Boolean = false

    init {
        itemsSelectedCount.value = 0
        setupTransferListener()
        cancellableJobs[JOB_KEY_MONITOR_SHUFFLE]?.cancel()
        cancellableJobs[JOB_KEY_MONITOR_SHUFFLE] = sharingScope.launch {
            monitorAudioShuffleEnabledUseCase().collect {
                recreateAndUpdatePlaylistItems(
                    originalItems = if (it) playlistItemsFlow.value.first else playlistItems
                )
            }
        }
    }

    override fun setPaused(paused: Boolean) {
        this.paused = paused
        mediaPlayback.value = paused
    }

    override suspend fun buildPlayerSource(intent: Intent?): Boolean {
        if (intent == null || !intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)) {
            retry.value = false
            return false
        }

        val type = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val uri = intent.data

        if (type == INVALID_VALUE || uri == null) {
            retry.value = false
            return false
        }

        val samePlaylist = isSamePlaylist(type, intent)
        currentIntent = intent

        val firstPlayHandle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
        if (firstPlayHandle == INVALID_HANDLE) {
            retry.value = false
            return false
        }

        val firstPlayNodeName = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME)
        if (firstPlayNodeName == null) {
            retry.value = false
            return false
        }

        // Because the same instance will be used if user creates another audio playlist,
        // so if we need stop streaming server in previous creation, we still need
        // stop it even if the new creation indicates we don't need to stop it,
        // otherwise the streaming server won't be stopped at the end.
        needStopStreamingServer = needStopStreamingServer || intent.getBooleanExtra(
            INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, false
        )

        playerRetry = 0

        // if we are already playing this music, then the metadata is already
        // in LiveData (_metadata of AudioPlayerService), we don't need (and shouldn't)
        // emit node name.
        val displayNodeNameFirst =
            type != OFFLINE_ADAPTER && !(samePlaylist && firstPlayHandle == playingHandle)

        val firstPlayUri = if (type == FOLDER_LINK_ADAPTER) {
            if (isMegaApiFolder(type)) {
                getLocalFolderLinkFromMegaApiFolderUseCase(firstPlayHandle)
            } else {
                getLocalFolderLinkFromMegaApiUseCase(firstPlayHandle)
            }?.let { url ->
                Uri.parse(url)
            }
        } else {
            uri
        }

        if (firstPlayUri == null) {
            retry.value = false
            return false
        }

        val mediaItem = MediaItem.Builder()
            .setUri(firstPlayUri)
            .setMediaId(firstPlayHandle.toString())
            .build()
        playerSource.value = MediaPlaySources(
            listOf(mediaItem),
            // we will emit a single item list at first, and the current playing item
            // will always be at index 0 in that single item list.
            if (samePlaylist && firstPlayHandle == playingHandle) 0 else INVALID_VALUE,
            if (displayNodeNameFirst) firstPlayNodeName else null
        )

        if (intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)) {
            if (type != OFFLINE_ADAPTER && type != ZIP_ADAPTER) {
                needStopStreamingServer =
                    needStopStreamingServer || setupStreamingServer(type)
            }
            cancellableJobs[JOB_KEY_BUILD_PLAYER_SOURCES]?.cancel()
            val buildPlayerSourcesJob = sharingScope.launch(ioDispatcher) {
                when (type) {
                    OFFLINE_ADAPTER -> {
                        playlistTitle.postValue(getOfflineFolderName(context, firstPlayHandle))
                        buildPlaylistFromOfflineNodes(intent, firstPlayHandle)
                    }

                    AUDIO_BROWSE_ADAPTER -> {
                        playlistTitle.postValue(context.getString(R.string.upload_to_audio))
                        buildPlaySourcesByTypedAudioNodes(
                            type = type,
                            typedAudioNodes = getAudioNodesUseCase(getSortOrderFromIntent(intent)),
                            firstPlayHandle = firstPlayHandle
                        )
                    }

                    FILE_BROWSER_ADAPTER,
                    RUBBISH_BIN_ADAPTER,
                    BACKUPS_ADAPTER,
                    LINKS_ADAPTER,
                    INCOMING_SHARES_ADAPTER,
                    OUTGOING_SHARES_ADAPTER,
                    CONTACT_FILE_ADAPTER,
                    -> {
                        val parentHandle = intent.getLongExtra(
                            INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                            INVALID_HANDLE
                        )
                        val order = getSortOrderFromIntent(intent)

                        if (isInRootLinksLevel(type, parentHandle)) {
                            playlistTitle.postValue(context.getString(R.string.tab_links_shares))
                            buildPlaySourcesByTypedAudioNodes(
                                type = type,
                                typedAudioNodes = getAudioNodesFromPublicLinksUseCase(order),
                                firstPlayHandle = firstPlayHandle
                            )
                            return@launch
                        }

                        if (type == INCOMING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE) {
                            playlistTitle.postValue(context.getString(R.string.tab_incoming_shares))
                            buildPlaySourcesByTypedAudioNodes(
                                type = type,
                                typedAudioNodes = getAudioNodesFromInSharesUseCase(order),
                                firstPlayHandle = firstPlayHandle
                            )
                            return@launch
                        }

                        if (type == OUTGOING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE) {
                            playlistTitle.postValue(context.getString(R.string.tab_outgoing_shares))
                            buildPlaySourcesByTypedAudioNodes(
                                type = type,
                                typedAudioNodes = getAudioNodesFromOutSharesUseCase(
                                    lastHandle = INVALID_HANDLE,
                                    order = order
                                ),
                                firstPlayHandle = firstPlayHandle
                            )
                            return@launch
                        }

                        if (type == CONTACT_FILE_ADAPTER && parentHandle == INVALID_HANDLE) {
                            intent.getStringExtra(INTENT_EXTRA_KEY_CONTACT_EMAIL)
                                ?.let { email ->
                                    getAudioNodesByEmailUseCase(email)?.let { nodes ->
                                        getUserNameByEmailUseCase(email)?.let {
                                            context.getString(R.string.title_incoming_shares_with_explorer)
                                                .let { sharesTitle ->
                                                    playlistTitle.postValue("$sharesTitle $it")
                                                }
                                        }
                                        buildPlaySourcesByTypedAudioNodes(
                                            type = type,
                                            typedAudioNodes = nodes,
                                            firstPlayHandle = firstPlayHandle
                                        )
                                    }
                                }
                            return@launch
                        }

                        if (parentHandle == INVALID_HANDLE) {
                            when (type) {
                                RUBBISH_BIN_ADAPTER -> getRubbishNodeUseCase()
                                BACKUPS_ADAPTER -> getBackupsNodeUseCase()
                                else -> getRootNodeUseCase()
                            }
                        } else {
                            getAudioNodeByHandleUseCase(parentHandle)
                        }?.let { parent ->
                            if (parentHandle == INVALID_HANDLE) {
                                context.getString(
                                    when (type) {
                                        RUBBISH_BIN_ADAPTER -> R.string.section_rubbish_bin
                                        BACKUPS_ADAPTER -> R.string.home_side_menu_backups_title
                                        else -> R.string.section_cloud_drive
                                    }
                                )
                            } else {
                                parent.name
                            }.let { title ->
                                playlistTitle.postValue(title)
                            }
                            getAudioNodesByParentHandleUseCase(
                                parentHandle = parent.id.longValue,
                                order = getSortOrderFromIntent(intent)
                            )?.let { children ->
                                buildPlaySourcesByTypedAudioNodes(
                                    type = type,
                                    typedAudioNodes = children,
                                    firstPlayHandle = firstPlayHandle
                                )
                            }
                        }
                    }

                    RECENTS_ADAPTER, RECENTS_BUCKET_ADAPTER -> {
                        playlistTitle.postValue(context.getString(R.string.section_recents))
                        intent.getLongArrayExtra(NODE_HANDLES)?.let { handles ->
                            buildPlaylistFromHandles(
                                type = type,
                                handles = handles.toList(),
                                firstPlayHandle = firstPlayHandle
                            )
                        }
                    }

                    FOLDER_LINK_ADAPTER -> {
                        val parentHandle = intent.getLongExtra(
                            INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                            INVALID_HANDLE
                        )
                        val order = getSortOrderFromIntent(intent)

                        (if (parentHandle == INVALID_HANDLE) {
                            getRootNodeFromMegaApiFolderUseCase()
                        } else {
                            getParentNodeFromMegaApiFolderUseCase(parentHandle)
                        })?.let { parent ->
                            playlistTitle.postValue(parent.name)

                            getAudiosByParentHandleFromMegaApiFolderUseCase(
                                parentHandle = parent.id.longValue,
                                order = order
                            )?.let { children ->
                                buildPlaySourcesByTypedAudioNodes(
                                    type = type,
                                    typedAudioNodes = children,
                                    firstPlayHandle = firstPlayHandle
                                )
                            }
                        }
                    }

                    ZIP_ADAPTER -> {
                        intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)
                            ?.let { zipPath ->
                                playlistTitle.postValue(File(zipPath).parentFile?.name ?: "")
                                File(zipPath).parentFile?.listFiles()?.let { files ->
                                    buildPlaySourcesByFiles(
                                        files = files.asList(),
                                        firstPlayHandle = firstPlayHandle
                                    )
                                }
                            }
                    }

                    SEARCH_BY_ADAPTER -> {
                        intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)
                            ?.let { handles ->
                                buildPlaylistFromHandles(
                                    type = type,
                                    handles = handles.toList(),
                                    firstPlayHandle = firstPlayHandle
                                )
                            }
                    }
                }
                postPlayingThumbnail()
            }
            cancellableJobs[JOB_KEY_BUILD_PLAYER_SOURCES] = buildPlayerSourcesJob
        } else {
            playlistItems.clear()

            val node = getAudioNodeByHandleUseCase(firstPlayHandle)
            val thumbnail = when {
                type == OFFLINE_ADAPTER -> {
                    offlineThumbnailFileWrapper.getThumbnailFile(
                        context,
                        firstPlayHandle.toString()
                    )
                }

                node == null -> {
                    null
                }

                else -> {
                    File(getThumbFolder(context), node.base64Id.plus(JPG_EXTENSION))
                }
            }

            val duration = node?.duration ?: 0.seconds

            playlistItemMapper(
                firstPlayHandle,
                firstPlayNodeName,
                thumbnail,
                0,
                TYPE_PLAYING,
                node?.size ?: INVALID_SIZE,
                duration,
            ).let { playlistItem ->
                playlistItems.add(playlistItem)
            }

            recreateAndUpdatePlaylistItems()

            if (thumbnail != null && !thumbnail.exists() && node != null) {
                runCatching {
                    if (isMegaApiFolder(type = type)) {
                        getThumbnailFromMegaApiFolderUseCase(
                            nodeHandle = node.id.longValue,
                            path = thumbnail.absolutePath
                        )?.let { nodeHandle ->
                            if (nodeHandle == playingHandle) {
                                postPlayingThumbnail()
                            }
                        }
                    } else {
                        getThumbnailFromMegaApiUseCase(
                            nodeHandle = node.id.longValue,
                            path = thumbnail.absolutePath
                        )?.let { nodeHandle ->
                            if (nodeHandle == playingHandle) {
                                postPlayingThumbnail()
                            }
                        }
                    }
                }.onFailure { Timber.e(it) }
            } else {
                postPlayingThumbnail()
            }
        }

        return true
    }

    /**
     * Build play sources by node OfflineNodes
     *
     * @param intent Intent
     * @param firstPlayHandle the index of first playing item
     */
    private fun buildPlaylistFromOfflineNodes(
        intent: Intent,
        firstPlayHandle: Long,
    ) {
        intent.parcelableArrayList<MegaOffline>(INTENT_EXTRA_KEY_ARRAY_OFFLINE)
            ?.let { offlineFiles ->
                playlistItems.clear()

                val mediaItems = mutableListOf<MediaItem>()
                var firstPlayIndex = 0

                offlineFiles.filter {
                    getOfflineFile(context, it).let { file ->
                        isFileAvailable(file) && file.isFile && filterByNodeName(it.name)
                    }
                }.mapIndexed { currentIndex, megaOffline ->
                    mediaItems.add(
                        mediaItemFromFile(getOfflineFile(context, megaOffline), megaOffline.handle)
                    )
                    if (megaOffline.handle.toLong() == firstPlayHandle) {
                        firstPlayIndex = currentIndex
                    }

                    playlistItemMapper(
                        megaOffline.handle.toLong(),
                        megaOffline.name,
                        offlineThumbnailFileWrapper.getThumbnailFile(context, megaOffline),
                        currentIndex,
                        TYPE_NEXT,
                        megaOffline.getSize(context),
                        0.seconds
                    )
                        .let { playlistItem ->
                            playlistItems.add(playlistItem)
                        }
                }

                updatePlaySources(mediaItems, playlistItems, firstPlayIndex)
            }
    }

    /**
     * Build play sources by node TypedNodes
     *
     * @param type adapter type
     * @param typedAudioNodes [TypedAudioNode] list
     * @param firstPlayHandle the index of first playing item
     */
    private suspend fun buildPlaySourcesByTypedAudioNodes(
        type: Int,
        typedAudioNodes: List<TypedAudioNode>,
        firstPlayHandle: Long,
    ) {
        playlistItems.clear()

        val mediaItems = ArrayList<MediaItem>()
        var firstPlayIndex = 0

        val nodesWithoutThumbnail = ArrayList<Pair<Long, File>>()

        typedAudioNodes.mapIndexed { currentIndex, typedAudioNode ->
            getLocalFilePathUseCase(typedAudioNode).let { localPath ->
                if (localPath != null && isLocalFile(typedAudioNode, localPath)) {
                    mediaItemFromFile(File(localPath), typedAudioNode.id.longValue.toString())
                } else {
                    val url =
                        if (type == FOLDER_LINK_ADAPTER) {
                            if (isMegaApiFolder(type)) {
                                getLocalFolderLinkFromMegaApiFolderUseCase(typedAudioNode.id.longValue)
                            } else {
                                getLocalFolderLinkFromMegaApiUseCase(typedAudioNode.id.longValue)
                            }
                        } else {
                            getLocalLinkFromMegaApiUseCase(typedAudioNode.id.longValue)
                        }
                    if (url == null) {
                        null
                    } else {
                        MediaItem.Builder()
                            .setUri(Uri.parse(url))
                            .setMediaId(typedAudioNode.id.longValue.toString())
                            .build()
                    }
                }?.let {
                    mediaItems.add(it)
                }
            }

            if (typedAudioNode.id.longValue == firstPlayHandle) {
                firstPlayIndex = currentIndex
            }
            val thumbnail = typedAudioNode.thumbnailPath?.let { path ->
                File(path)
            }

            val duration = typedAudioNode.duration

            playlistItemMapper(
                typedAudioNode.id.longValue,
                typedAudioNode.name,
                thumbnail,
                currentIndex,
                TYPE_NEXT,
                typedAudioNode.size,
                duration,
            ).let { playlistItem ->
                playlistItems.add(playlistItem)
            }

            if (thumbnail != null && !thumbnail.exists()) {
                nodesWithoutThumbnail.add(Pair(typedAudioNode.id.longValue, thumbnail))
            }
        }

        if (nodesWithoutThumbnail.isNotEmpty() && isConnectedToInternetUseCase()) {
            cancellableJobs[JOB_KEY_UPDATE_THUMBNAIL]?.cancel()
            val updateThumbnailJob = sharingScope.launch(ioDispatcher) {
                nodesWithoutThumbnail.map {
                    runCatching {
                        if (isMegaApiFolder(type = type)) {
                            getThumbnailFromMegaApiFolderUseCase(
                                nodeHandle = it.first,
                                path = it.second.absolutePath
                            )?.let { nodeHandle ->
                                if (nodeHandle == playingHandle) {
                                    postPlayingThumbnail()
                                }
                            }
                        } else {
                            getThumbnailFromMegaApiUseCase(
                                nodeHandle = it.first,
                                path = it.second.absolutePath
                            )?.let { nodeHandle ->
                                if (nodeHandle == playingHandle) {
                                    postPlayingThumbnail()
                                }
                            }
                        }
                    }.onFailure { Timber.e(it) }
                }
            }
            cancellableJobs[JOB_KEY_UPDATE_THUMBNAIL] = updateThumbnailJob
        }
        updatePlaySources(mediaItems, playlistItems, firstPlayIndex)
    }

    /**
     * Build play sources by node handles
     *
     * @param type adapter type
     * @param handles node handles
     * @param firstPlayHandle the index of first playing item
     */
    private suspend fun buildPlaylistFromHandles(
        type: Int,
        handles: List<Long>,
        firstPlayHandle: Long,
    ) {
        buildPlaySourcesByTypedAudioNodes(
            type = type,
            typedAudioNodes = getAudioNodesByHandlesUseCase(handles),
            firstPlayHandle = firstPlayHandle
        )
    }

    /**
     * Build play sources by files
     *
     * @param files media files
     * @param firstPlayHandle the index of first playing item
     */
    private fun buildPlaySourcesByFiles(
        files: List<File>,
        firstPlayHandle: Long,
    ) {
        playlistItems.clear()

        val mediaItems = ArrayList<MediaItem>()
        var firstPlayIndex = 0

        files.filter {
            it.isFile && filterByNodeName(it.name)
        }.mapIndexed { currentIndex, file ->
            mediaItems.add(mediaItemFromFile(file, file.name.hashCode().toString()))

            if (file.name.hashCode().toLong() == firstPlayHandle) {
                firstPlayIndex = currentIndex
            }

            playlistItemMapper(
                file.name.hashCode().toLong(),
                file.name,
                null,
                currentIndex,
                TYPE_NEXT,
                file.length(),
                0.seconds
            )
                .let { playlistItem ->
                    playlistItems.add(playlistItem)
                }
        }
        updatePlaySources(mediaItems, playlistItems, firstPlayIndex)
    }

    /**
     * Update play sources for media player and playlist
     *
     * @param mediaItems media items
     * @param items playlist items
     * @param firstPlayIndex the index of first playing item
     */
    private fun updatePlaySources(
        mediaItems: List<MediaItem>,
        items: List<PlaylistItem>,
        firstPlayIndex: Int,
    ) {
        if (mediaItems.isNotEmpty() && items.isNotEmpty()) {
            playerSource.postValue(MediaPlaySources(mediaItems, firstPlayIndex, null))
            recreateAndUpdatePlaylistItems(originalItems = items)
        }
    }

    /**
     * Setup transfer listener
     */
    private fun setupTransferListener() {
        cancellableJobs[JOB_KEY_MONITOR_TRANSFER]?.cancel()
        cancellableJobs[JOB_KEY_MONITOR_TRANSFER] = sharingScope.launch {
            monitorTransferEventsUseCase()
                .catch {
                    Timber.e(it)
                }.collect { event ->
                    if (event is TransferEvent.TransferTemporaryErrorEvent) {
                        val megaException = event.error
                        val transfer = event.transfer
                        if (transfer.nodeHandle == playingHandle
                            && ((megaException is QuotaExceededMegaException
                                    && !transfer.isForeignOverQuota
                                    && megaException.value != 0L)
                                    || megaException is BlockedMegaException)
                        ) {
                            error.postValue(megaException)
                        }
                    }
                }
        }
    }

    override fun onPlayerError() {
        playerRetry++
        retry.value = playerRetry <= MAX_RETRY
    }

    /**
     * Check if the new intent would create the same playlist as current one.
     *
     * @param type new adapter type
     * @param intent new intent
     * @return if the new intent would create the same playlist as current one
     */
    private fun isSamePlaylist(type: Int, intent: Intent): Boolean {
        val oldIntent = currentIntent ?: return false
        val oldType = oldIntent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)

        if (
            intent.getBooleanExtra(INTENT_EXTRA_KEY_FROM_DOWNLOAD_SERVICE, false)
            && oldIntent.getBooleanExtra(INTENT_EXTRA_KEY_FROM_DOWNLOAD_SERVICE, false)
        ) {
            return true
        }

        when (type) {
            OFFLINE_ADAPTER -> {
                val oldDir = oldIntent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)
                    ?: return false
                val newDir =
                    intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY) ?: return false
                return oldDir == newDir
            }

            AUDIO_BROWSE_ADAPTER,
            FROM_CHAT,
            FILE_LINK_ADAPTER,
            PHOTO_SYNC_ADAPTER,
            -> {
                return oldType == type
            }

            FILE_BROWSER_ADAPTER,
            RUBBISH_BIN_ADAPTER,
            BACKUPS_ADAPTER,
            LINKS_ADAPTER,
            INCOMING_SHARES_ADAPTER,
            OUTGOING_SHARES_ADAPTER,
            CONTACT_FILE_ADAPTER,
            FOLDER_LINK_ADAPTER,
            -> {
                val oldParentHandle = oldIntent.getLongExtra(
                    INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                    INVALID_HANDLE
                )
                val newParentHandle = intent.getLongExtra(
                    INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                    INVALID_HANDLE
                )
                return oldType == type && oldParentHandle == newParentHandle
            }

            RECENTS_ADAPTER, RECENTS_BUCKET_ADAPTER -> {
                val oldHandles = oldIntent.getLongArrayExtra(NODE_HANDLES) ?: return false
                val newHandles = intent.getLongArrayExtra(NODE_HANDLES) ?: return false
                return oldHandles.contentEquals(newHandles)
            }

            ZIP_ADAPTER -> {
                val oldZipPath = oldIntent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)
                    ?: return false
                val newZipPath =
                    intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY) ?: return false
                return oldZipPath == newZipPath
            }

            SEARCH_BY_ADAPTER -> {
                val oldHandles = oldIntent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)
                    ?: return false
                val newHandles =
                    intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH) ?: return false
                return oldHandles.contentEquals(newHandles)
            }

            else -> {
                return false
            }
        }
    }

    private fun filterByNodeName(name: String): Boolean =
        MimeTypeList.typeForName(name).let { mime ->
            mime.isAudio && !mime.isAudioNotSupported
        }

    private fun getSortOrderFromIntent(intent: Intent): SortOrder {
        val order =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(
                    INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                    SortOrder::class.java
                ) ?: SortOrder.ORDER_DEFAULT_ASC
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(
                    INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
                ) as SortOrder?
                    ?: SortOrder.ORDER_DEFAULT_ASC
            }
        return order
    }

    override fun initNewSearch(): MegaCancelToken {
        cancelSearch()
        return MegaCancelToken.createInstance()
    }

    override fun cancelSearch() {
        cancelToken?.cancel()
    }

    private fun mediaItemFromFile(file: File, handle: String): MediaItem =
        MediaItem.Builder()
            .setUri(getUriForFile(context, file))
            .setMediaId(handle)
            .build()


    private fun postPlayingThumbnail() =
        playlistItems.toList().firstOrNull { (nodeHandle) ->
            nodeHandle == playingHandle
        }?.thumbnail?.let {
            playingThumbnail.postValue(it)
        }

    /**
     * Recreate and update playlist items
     *
     * @param originalItems playlist items
     * @param isScroll true is scroll to target position, otherwise is false.
     * @param isBuildPlaySources true is building play sources, otherwise is false.
     */
    @OptIn(UnstableApi::class)
    private fun recreateAndUpdatePlaylistItems(
        originalItems: List<PlaylistItem?> = playlistItems,
        isScroll: Boolean = true,
        isBuildPlaySources: Boolean = true,
    ) {
        cancellableJobs[JOB_KEY_UPDATE_PLAYLIST]?.cancel()
        val updatePlaylistJob = sharingScope.launch(ioDispatcher) {
            Timber.d("recreateAndUpdatePlaylistItems ${originalItems.size} items")
            if (originalItems.isEmpty()) {
                return@launch
            }
            val items = originalItems.filterNotNull()
            playingPosition = items.indexOfFirst { (nodeHandle) ->
                nodeHandle == playingHandle
            }.takeIf { index ->
                index in originalItems.indices
            } ?: 0

            val recreatedItems = mutableListOf<PlaylistItem>()
            // Adjust whether need to build play sources again to avoid playlist is reordered everytime playlist items updated
            if (isBuildPlaySources && shuffleEnabled.value && shuffleOrder.length == originalItems.size) {
                recreatedItems.add(items[playingPosition])

                var newPlayingIndex = 0

                var index = shuffleOrder.getPreviousIndex(playingPosition)
                while (index != C.INDEX_UNSET) {
                    recreatedItems.add(0, items[index])
                    index = shuffleOrder.getPreviousIndex(index)
                    newPlayingIndex++
                }

                index = shuffleOrder.getNextIndex(playingPosition)
                while (index != C.INDEX_UNSET) {
                    recreatedItems.add(items[index])
                    index = shuffleOrder.getNextIndex(index)
                }

                playingPosition = newPlayingIndex
            } else {
                recreatedItems.addAll(items)
            }

            val searchQuery = playlistSearchQuery
            if (isSearchMode && !searchQuery.isNullOrEmpty()) {
                filterPlaylistItems(recreatedItems, searchQuery)
            } else {
                for ((index, item) in recreatedItems.withIndex()) {
                    val type = when {
                        index < playingPosition -> TYPE_PREVIOUS
                        playingPosition == index -> TYPE_PLAYING
                        else -> TYPE_NEXT
                    }
                    recreatedItems[index] =
                        item.finalizeItem(
                            index = index,
                            type = type,
                            isSelected = item.isSelected,
                            duration = item.duration,
                        )
                }

                if (playingPosition > 0) {
                    recreatedItems[0] = recreatedItems[0].copy(headerIsVisible = true)
                }
                recreatedItems[playingPosition] =
                    recreatedItems[playingPosition].copy(headerIsVisible = true)

                recreatedItems
            }.let { updatedList ->
                Timber.d("recreateAndUpdatePlaylistItems post ${updatedList.size} items")
                val scrollPosition = if (isScroll) {
                    playingPosition
                } else {
                    -1
                }
                playlistItemsFlow.update {
                    it.copy(updatedList, scrollPosition)
                }
            }
        }
        cancellableJobs[JOB_KEY_UPDATE_PLAYLIST] = updatePlaylistJob
    }

    private fun filterPlaylistItems(
        items: List<PlaylistItem>,
        filter: String,
    ): MutableList<PlaylistItem> {
        val filteredItems = ArrayList<PlaylistItem>()
        items.forEachIndexed { index, item ->
            if (item.nodeName.contains(filter, true)) {
                // Filter only affects displayed playlist, it doesn't affect what
                // ExoPlayer is playing, so we still need use the index before filter.
                filteredItems.add(item.finalizeItem(index, TYPE_PREVIOUS))
            }
        }
        return filteredItems.toMutableList()
    }

    override fun searchQueryUpdate(newText: String?) {
        playlistSearchQuery = newText
        recreateAndUpdatePlaylistItems(isBuildPlaySources = false)
    }

    override fun getCurrentIntent() = currentIntent

    override fun getCurrentPlayingHandle() = playingHandle

    override fun setCurrentPlayingHandle(handle: Long) {
        playingHandle = handle
        playlistItemsFlow.value.first.let { playlistItems ->
            playingPosition = playlistItems.indexOfFirst { (nodeHandle) ->
                nodeHandle == handle
            }.takeIf { index -> index in playlistItems.indices } ?: 0
            recreateAndUpdatePlaylistItems(
                originalItems = playlistItemsFlow.value.first,
                isBuildPlaySources = false
            )
        }
        postPlayingThumbnail()
    }

    override fun getPlaylistItem(handle: String?): PlaylistItem? =
        handle?.let {
            playlistItems.toList().firstOrNull { (nodeHandle) ->
                nodeHandle == handle.toLong()
            }
        }

    override fun getPlayingThumbnail() = playingThumbnail

    override fun playerSourceUpdate() = playerSource.asFlow()

    override fun mediaItemToRemoveUpdate() = mediaItemToRemove

    override fun nodeNameUpdate() = nodeNameUpdate.asFlow()

    override fun retryUpdate() = retry.asFlow()

    override fun playlistUpdate() = playlistItemsFlow

    override fun mediaPlaybackUpdate() = mediaPlayback.asFlow()

    override fun errorUpdate() = error.asFlow()

    override fun itemsClearedUpdate() = itemsClearedState

    override fun playlistTitleUpdate() = playlistTitle.asFlow()

    override fun itemsSelectedCountUpdate() = itemsSelectedCount.asFlow()

    override fun actionModeUpdate() = actionMode.asFlow()

    override fun removeItem(handle: Long) {
        initPlayerSourceChanged()
        val newItems = removeSingleItem(handle)
        if (newItems.isNotEmpty()) {
            resetRetryState()
            recreateAndUpdatePlaylistItems(originalItems = newItems)
        } else {
            playlistItemsFlow.update {
                it.copy(emptyList(), 0)
            }
            itemsClearedState.update { true }
        }
    }

    private fun removeSingleItem(handle: Long): List<PlaylistItem> =
        playlistItemsFlow.value.first.let { items ->
            val newItems = items.toMutableList()
            items.indexOfFirst { (nodeHandle) ->
                nodeHandle == handle
            }.takeIf { index ->
                index in playlistItems.indices
            }?.let { index ->
                cancellableJobs[JOB_KEY_REMOVE_ITEM]?.cancel()
                cancellableJobs[JOB_KEY_REMOVE_ITEM] = sharingScope.launch {
                    mediaItemToRemove.emit(index)
                }
                newItems.removeIf { (nodeHandle) ->
                    nodeHandle == handle
                }
                playlistItems.removeIf { (nodeHandle) ->
                    nodeHandle == handle
                }
                playSourceChanged.removeIf { mediaItem ->
                    mediaItem.mediaId.toLong() == handle
                }
            }
            newItems
        }

    override fun removeAllSelectedItems() {
        if (itemsSelectedMap.isNotEmpty()) {
            itemsSelectedMap.forEach {
                removeSingleItem(it.value.nodeHandle).let { newItems ->
                    playlistItemsFlow.update { flow ->
                        flow.copy(newItems, playingPosition)
                    }
                }
            }
            itemsSelectedMap.clear()
            itemsSelectedCount.value = itemsSelectedMap.size
            actionMode.value = false
        }
    }

    override fun itemSelected(handle: Long) {
        playlistItemsFlow.update {
            it.copy(
                it.first.toMutableList().let { playlistItems ->
                    playlistItems.indexOfFirst { (nodeHandle) ->
                        nodeHandle == handle
                    }.takeIf { index ->
                        index in playlistItems.indices
                    }?.let { selectedIndex ->
                        playlistItems[selectedIndex].let { item ->
                            val isSelected = !item.isSelected
                            playlistItems[selectedIndex] = item.copy(isSelected = isSelected)
                            if (playlistItems[selectedIndex].isSelected) {
                                itemsSelectedMap[handle] = item
                            } else {
                                itemsSelectedMap.remove(handle)
                            }
                            itemsSelectedCount.value = itemsSelectedMap.size
                        }
                    }
                    playlistItems
                }
            )
        }
    }

    override fun clearSelections() {
        playlistItemsFlow.update {
            it.copy(
                it.first.toMutableList().let { playlistItems ->
                    playlistItems.map { item ->
                        item.copy(isSelected = false)
                    }
                }
            )
        }
        itemsSelectedMap.clear()
        actionMode.value = false

    }

    override fun setActionMode(isActionMode: Boolean) {
        actionMode.value = isActionMode
        if (isActionMode) {
            recreateAndUpdatePlaylistItems(
                originalItems = playlistItemsFlow.value.first,
                isScroll = false,
                isBuildPlaySources = false
            )
        }
    }

    override fun resetRetryState() {
        playerRetry = 0
        retry.value = true
    }

    override fun updateItemName(handle: Long, newName: String) =
        playlistItemsFlow.update {
            it.copy(
                it.first.map { item ->
                    if (item.nodeHandle == handle) {
                        nodeNameUpdate.postValue(newName)
                        item.updateNodeName(newName)
                    } else {
                        item
                    }
                }
            )
        }

    override fun getPlaylistItems() = playlistItemsFlow.value.first

    override fun backgroundPlayEnabled() = backgroundPlayEnabled.value

    override fun toggleBackgroundPlay(isEnable: Boolean): Boolean {
        cancellableJobs[JOB_KEY_TOGGLE_BACKGROUND_PLAY]?.cancel()
        cancellableJobs[JOB_KEY_TOGGLE_BACKGROUND_PLAY] = sharingScope.launch {
            setAudioBackgroundPlayEnabledUseCase(isEnable)
        }
        return isEnable
    }

    override fun shuffleEnabled(): Boolean = shuffleEnabled.value

    override fun getShuffleOrder() = shuffleOrder

    override fun setShuffleEnabled(enabled: Boolean) {
        cancellableJobs[JOB_KEY_SET_SHUFFLE]?.cancel()
        cancellableJobs[JOB_KEY_SET_SHUFFLE] = sharingScope.launch {
            setAudioShuffleEnabledUseCase(enabled)
        }
    }

    override fun newShuffleOrder(): ShuffleOrder {
        shuffleOrder = ExposedShuffleOrder(playlistItemsFlow.value.first.size, this)
        return shuffleOrder
    }

    override fun audioRepeatToggleMode() = audioRepeatToggleMode.value

    override fun setAudioRepeatMode(repeatToggleMode: RepeatToggleMode) {
        cancellableJobs[JOB_KEY_SET_AUDIO_REPEAT_MODE]?.cancel()
        cancellableJobs[JOB_KEY_SET_AUDIO_REPEAT_MODE] = sharingScope.launch {
            setAudioRepeatModeUseCase(repeatToggleMode.ordinal)
        }
    }

    override fun clear() {
        sharingScope.launch {
            compositeDisposable.dispose()

            if (needStopStreamingServer) {
                megaApiHttpServerStop()
                megaApiFolderHttpServerStopUseCase()
            }
        }
        cancellableJobs.values.map {
            it.cancel()
        }
    }

    private suspend fun isMegaApiFolder(type: Int) =
        type == FOLDER_LINK_ADAPTER && areCredentialsNullUseCase()

    override fun swapItems(current: Int, target: Int) {
        if (playlistItemsChanged.isEmpty()) {
            playlistItemsChanged.addAll(playlistItemsFlow.value.first)
        }
        Collections.swap(playlistItemsChanged, current, target)
        val index = playlistItemsChanged[current].index
        playlistItemsChanged[current] =
            playlistItemsChanged[current].copy(index = playlistItemsChanged[target].index)
        playlistItemsChanged[target] = playlistItemsChanged[target].copy(index = index)

        initPlayerSourceChanged()
        // Swap the items of play source
        Collections.swap(playSourceChanged, current, target)
    }

    override fun getIndexFromPlaylistItems(item: PlaylistItem): Int? =
        /* The media items of ExoPlayer are still the original order even the shuffleEnable is true,
         so the index of media item should be got from original playlist items */
        playlistItems.indexOfFirst {
            it.nodeHandle == item.nodeHandle
        }.takeIf { index ->
            index in playlistItems.indices
        }

    override fun updatePlaySource() {
        playlistItemsFlow.update {
            it.copy(playlistItemsChanged.toList())
        }
        playerSource.value?.run {
            playerSource.value =
                copy(
                    mediaItems = playSourceChanged.toList(),
                    newIndexForCurrentItem = playingPosition
                )
        }
        playSourceChanged.clear()
        playlistItemsChanged.clear()
    }

    override fun isPaused() = paused

    override fun getPlayingPosition(): Int = playingPosition

    override fun scrollToPlayingPosition() =
        recreateAndUpdatePlaylistItems(
            originalItems = playlistItemsFlow.value.first,
            isBuildPlaySources = false
        )

    override fun isActionMode() = actionMode.value

    private fun initPlayerSourceChanged() {
        if (playSourceChanged.isEmpty()) {
            // Get the play source
            playerSource.value?.run {
                playSourceChanged.addAll(mediaItems)
            }
        }
    }

    /**
     * onShuffleChanged
     */
    @OptIn(UnstableApi::class)
    override fun onShuffleChanged(newShuffle: ShuffleOrder) {
        shuffleOrder = newShuffle
        if (shuffleEnabled.value && shuffleOrder.length != 0 && shuffleOrder.length == playlistItems.size) {
            recreateAndUpdatePlaylistItems()
        }
    }

    private suspend fun setupStreamingServer(type: Int): Boolean {
        if (isMegaApiFolder(type)) {
            if (megaApiFolderHttpServerIsRunningUseCase() != 0) {
                return false
            }
            megaApiFolderHttpServerStartUseCase()
        } else {
            if (megaApiHttpServerIsRunningUseCase() != 0) {
                return false
            }
            megaApiHttpServerStartUseCase()
        }

        return true
    }

    private suspend fun isLocalFile(node: TypedFileNode, localPath: String?): Boolean =
        node.fingerprint.let { fingerprint ->
            localPath != null &&
                    (isOnMegaDownloads(node) || (fingerprint != null
                            && fingerprint == getFingerprintUseCase(localPath)))
        }

    private fun isOnMegaDownloads(node: TypedFileNode): Boolean =
        File(getDownloadLocation(), node.name).let { file ->
            isFileAvailable(file) && file.length() == node.size
        }

    override fun setSearchMode(value: Boolean) {
        isSearchMode = value
    }

    companion object {
        private const val MAX_RETRY = 6

        private const val JOB_KEY_MONITOR_SHUFFLE = "JOB_KEY_MONITOR_SHUFFLE"
        private const val JOB_KEY_BUILD_PLAYER_SOURCES = "KEY_JOB_BUILD_PLAYER_SOURCES"
        private const val JOB_KEY_UPDATE_THUMBNAIL = "JOB_KEY_UPDATE_THUMBNAIL"
        private const val JOB_KEY_UPDATE_PLAYLIST = "KEY_JOB_UPDATE_PLAYLIST"
        private const val JOB_KEY_REMOVE_ITEM = "JOB_KEY_REMOVE_ITEM"
        private const val JOB_KEY_TOGGLE_BACKGROUND_PLAY = "JOB_KEY_TOGGLE_BACKGROUND_PLAY"
        private const val JOB_KEY_SET_SHUFFLE = "JOB_KEY_SET_SHUFFLE"
        private const val JOB_KEY_SET_AUDIO_REPEAT_MODE = "JOB_KEY_SET_AUDIO_REPEAT_MODE"
        private const val JOB_KEY_MONITOR_TRANSFER = "JOB_KEY_MONITOR_TRANSFER"
    }
}
