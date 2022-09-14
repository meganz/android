package mega.privacy.android.app.mediaplayer.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ShuffleOrder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.SettingsConstants.KEY_AUDIO_BACKGROUND_PLAY_ENABLED
import mega.privacy.android.app.constants.SettingsConstants.KEY_AUDIO_REPEAT_MODE
import mega.privacy.android.app.constants.SettingsConstants.KEY_AUDIO_SHUFFLE_ENABLED
import mega.privacy.android.app.constants.SettingsConstants.KEY_VIDEO_REPEAT_MODE
import mega.privacy.android.app.listeners.MegaRequestFinishListener
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.RepeatToggleMode
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.search.callback.SearchCallback
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase.Result
import mega.privacy.android.app.utils.Constants.AUDIO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.Constants.FILE_BROWSER_ADAPTER
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.INBOX_ADAPTER
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
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.ContactUtil.getMegaUserNameDB
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.FileUtil.getLocalFile
import mega.privacy.android.app.utils.FileUtil.getUriForFile
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.FileUtil.isLocalFile
import mega.privacy.android.app.utils.MegaNodeUtil.isInRootLinksLevel
import mega.privacy.android.app.utils.MegaNodeUtil.setupStreamingServer
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFolderName
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.ThumbnailUtils.getThumbFolder
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.FILE_TYPE_AUDIO
import nz.mega.sdk.MegaApiJava.FILE_TYPE_VIDEO
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.SEARCH_TARGET_ROOTNODE
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import org.jetbrains.anko.defaultSharedPreferences
import timber.log.Timber
import java.io.File
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

/**
 * A class containing audio player service logic, because using ViewModel in Service
 * is not the standard scenario, so this class is actually not a subclass of ViewModel.
 */
class MediaPlayerServiceViewModel(
    private val context: Context,
    private val megaApi: MegaApiAndroid,
    private val megaApiFolder: MegaApiAndroid,
    private val dbHandler: DatabaseHandler,
    private val offlineThumbnailFileWrapper: GetOfflineThumbnailFileWrapper,
    private val getGlobalTransferUseCase: GetGlobalTransferUseCase,
) : PlayerServiceViewModelGateway, ExposedShuffleOrder.ShuffleChangeListener, SearchCallback.Data {
    private val compositeDisposable = CompositeDisposable()

    private val preferences = context.defaultSharedPreferences
    private var backgroundPlayEnabled =
        preferences.getBoolean(KEY_AUDIO_BACKGROUND_PLAY_ENABLED, true)
    private var shuffleEnabled = preferences.getBoolean(KEY_AUDIO_SHUFFLE_ENABLED, false)
    private var videoRepeatToggleMode =
        convertToRepeatToggleMode(preferences.getInt(KEY_VIDEO_REPEAT_MODE,
            RepeatToggleMode.REPEAT_NONE.ordinal))
    private var audioRepeatToggleMode =
        convertToRepeatToggleMode(preferences.getInt(KEY_AUDIO_REPEAT_MODE,
            RepeatToggleMode.REPEAT_NONE.ordinal))


    private val createThumbnailFinished = PublishSubject.create<Boolean>()
    private val createThumbnailRequest = MegaRequestFinishListener({
        createThumbnailFinished.onNext(true)

        if (it.nodeHandle == playingHandle) {
            postPlayingThumbnail()
        }
    })

    private var currentIntent: Intent? = null

    private val playerSource = MutableLiveData<MediaPlaySources>()

    private val mediaItemToRemove = MutableLiveData<Int>()

    private val nodeNameUpdate = MutableLiveData<String>()

    private val playingThumbnail = MutableLiveData<File>()

    private val playlist = MutableLiveData<Pair<List<PlaylistItem>, Int>>()

    private val playlistTitle = MutableLiveData<String>()

    private val retry = MutableLiveData<Boolean>()

    private val error = MutableLiveData<Int>()

    private var actionMode = MutableLiveData<Boolean>()

    private val itemsSelectedCount = MutableLiveData<Int>()

    private var mediaPlayback = MutableLiveData<Boolean>()

    private val playlistItems = mutableListOf<PlaylistItem>()
    private val playlistItemsMap = mutableMapOf<String, PlaylistItem>()
    private val itemsSelectedMap = mutableMapOf<Long, PlaylistItem>()

    private var playlistSearchQuery: String? = null

    private var shuffleOrder: ShuffleOrder = ExposedShuffleOrder(0, this)

    private var playingHandle = INVALID_HANDLE

    private var paused = false

    private var isAudioPlayer = false

    private var playerRetry = 0

    private var needStopStreamingServer = false

    private var playSourceChanged: MutableList<MediaItem> = mutableListOf()
    private var playingPosition = 0

    private var cancelToken: MegaCancelToken? = null

    init {
        compositeDisposable.add(
            createThumbnailFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe(
                    { postPlaylistItems() },
                    logErr("AudioPlayerServiceViewModel creatingThumbnailFinished")
                )
        )
        itemsSelectedCount.value = 0
        setupTransferListener()
    }

    override fun setPaused(paused: Boolean, currentPosition: Long?) {
        this.paused = paused
        postPlaylistItems(currentPosition = currentPosition, isScroll = false)
        mediaPlayback.value = paused
    }

    override fun buildPlayerSource(intent: Intent?): Boolean {
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
        isAudioPlayer = MimeTypeList.typeForName(firstPlayNodeName).isAudio

        var displayNodeNameFirst = type != OFFLINE_ADAPTER || !isAudioPlayer
        if (samePlaylist && firstPlayHandle == playingHandle) {
            // if we are already playing this music, then the metadata is already
            // in LiveData (_metadata of AudioPlayerService), we don't need (and shouldn't)
            // emit node name.
            displayNodeNameFirst = false
        }

        val firstPlayUri = if (type == FOLDER_LINK_ADAPTER) {
            val node = megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(firstPlayHandle))
            if (node == null) {
                null
            } else {
                val url = getApi(type).httpServerGetLocalLink(node)
                if (url == null) {
                    null
                } else {
                    Uri.parse(url)
                }
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
                    needStopStreamingServer || setupStreamingServer(getApi(type), context)
            }

            compositeDisposable.add(
                Completable
                    .fromCallable(Callable {
                        when (type) {
                            OFFLINE_ADAPTER -> {
                                playlistTitle.postValue(
                                    getOfflineFolderName(context, firstPlayHandle)
                                )

                                buildPlaylistFromOfflineNodes(intent, firstPlayHandle)
                            }
                            AUDIO_BROWSE_ADAPTER -> {
                                playlistTitle.postValue(
                                    getString(R.string.upload_to_audio)
                                )

                                buildPlaylistForAudio(intent, firstPlayHandle)
                            }
                            VIDEO_BROWSE_ADAPTER -> {
                                playlistTitle.postValue(
                                    getString(R.string.sortby_type_video_first)
                                )

                                buildPlaylistForVideos(intent, firstPlayHandle)
                            }
                            FILE_BROWSER_ADAPTER,
                            RUBBISH_BIN_ADAPTER,
                            INBOX_ADAPTER,
                            LINKS_ADAPTER,
                            INCOMING_SHARES_ADAPTER,
                            OUTGOING_SHARES_ADAPTER,
                            CONTACT_FILE_ADAPTER,
                            -> {
                                val parentHandle = intent.getLongExtra(
                                    INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                                    INVALID_HANDLE
                                )
                                val order = intent.getIntExtra(
                                    INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                                    ORDER_DEFAULT_ASC
                                )

                                if (isInRootLinksLevel(type, parentHandle)) {
                                    playlistTitle.postValue(
                                        getString(R.string.tab_links_shares)
                                    )

                                    buildPlaylistFromNodes(
                                        megaApi, megaApi.getPublicLinks(order), firstPlayHandle
                                    )
                                    return@Callable
                                }

                                if (type == INCOMING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE) {
                                    playlistTitle.postValue(
                                        getString(R.string.tab_incoming_shares)
                                    )

                                    buildPlaylistFromNodes(
                                        megaApi, megaApi.getInShares(order), firstPlayHandle
                                    )
                                    return@Callable
                                }

                                if (type == OUTGOING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE) {
                                    playlistTitle.postValue(
                                        getString(R.string.tab_outgoing_shares)
                                    )

                                    val nodes = mutableListOf<MegaNode>()
                                    var lastHandle = INVALID_HANDLE
                                    for (share in megaApi.getOutShares(order)) {
                                        val node = megaApi.getNodeByHandle(share.nodeHandle)
                                        if (node != null && node.handle != lastHandle) {
                                            lastHandle = node.handle
                                            nodes.add(node)
                                        }
                                    }

                                    buildPlaylistFromNodes(megaApi, nodes, firstPlayHandle)
                                    return@Callable
                                }

                                if (type == CONTACT_FILE_ADAPTER && parentHandle == INVALID_HANDLE) {
                                    val email =
                                        intent.getStringExtra(INTENT_EXTRA_KEY_CONTACT_EMAIL)
                                    val contact = megaApi.getContact(email) ?: return@Callable
                                    val nodes = megaApi.getInShares(contact)

                                    val title =
                                        getString(R.string.title_incoming_shares_with_explorer) +
                                                " " + getMegaUserNameDB(contact)

                                    playlistTitle.postValue(title)

                                    buildPlaylistFromNodes(megaApi, nodes, firstPlayHandle)
                                    return@Callable
                                }

                                val parent = (if (parentHandle == INVALID_HANDLE) {
                                    when (type) {
                                        RUBBISH_BIN_ADAPTER -> megaApi.rubbishNode
                                        INBOX_ADAPTER -> megaApi.inboxNode
                                        else -> megaApi.rootNode
                                    }
                                } else {
                                    megaApi.getNodeByHandle(parentHandle)
                                }) ?: return@Callable

                                val title = if (parentHandle == INVALID_HANDLE) {
                                    getString(
                                        when (type) {
                                            RUBBISH_BIN_ADAPTER -> R.string.section_rubbish_bin
                                            INBOX_ADAPTER -> R.string.section_inbox
                                            else -> R.string.section_cloud_drive
                                        }
                                    )
                                } else {
                                    parent.name
                                }

                                playlistTitle.postValue(title)

                                buildPlaylistFromParent(parent, intent, firstPlayHandle)
                            }
                            RECENTS_ADAPTER, RECENTS_BUCKET_ADAPTER -> {
                                playlistTitle.postValue(
                                    getString(R.string.section_recents)
                                )

                                val handles =
                                    intent.getLongArrayExtra(NODE_HANDLES) ?: return@Callable
                                buildPlaylistFromHandles(handles.toList(), firstPlayHandle)
                            }
                            FOLDER_LINK_ADAPTER -> {
                                val parentHandle = intent.getLongExtra(
                                    INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                                    INVALID_HANDLE
                                )
                                val order = intent.getIntExtra(
                                    INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                                    ORDER_DEFAULT_ASC
                                )

                                val parent = (if (parentHandle == INVALID_HANDLE) {
                                    megaApiFolder.rootNode
                                } else {
                                    megaApiFolder.getNodeByHandle(parentHandle)
                                }) ?: return@Callable

                                playlistTitle.postValue(parent.name)

                                buildPlaylistFromNodes(
                                    getApi(type), megaApiFolder.getChildren(parent, order),
                                    firstPlayHandle, true
                                )
                            }
                            ZIP_ADAPTER -> {
                                val zipPath =
                                    intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)
                                        ?: return@Callable

                                playlistTitle.postValue(File(zipPath).parentFile?.name ?: "")

                                val files = File(zipPath).parentFile?.listFiles() ?: return@Callable
                                buildPlaylistFromFiles(files.asList(), firstPlayHandle)
                            }
                            SEARCH_BY_ADAPTER -> {
                                val handles =
                                    intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)
                                        ?: return@Callable
                                buildPlaylistFromHandles(handles.toList(), firstPlayHandle)
                            }
                        }

                        postPlayingThumbnail()
                    })
                    .subscribeOn(Schedulers.single())
                    .subscribe(IGNORE, logErr("AudioPlayerServiceViewModel buildPlayerSource"))
            )
        } else {
            playlistItems.clear()
            playlistItemsMap.clear()

            val node = megaApi.getNodeByHandle(firstPlayHandle)
            val thumbnail = when {
                type == OFFLINE_ADAPTER -> {
                    offlineThumbnailFileWrapper.getThumbnailFile(context,
                        firstPlayHandle.toString())
                }
                node == null -> {
                    null
                }
                else -> {
                    File(getThumbFolder(context), node.base64Handle.plus(JPG_EXTENSION))
                }
            }

            val playlistItem = PlaylistItem(
                nodeHandle = firstPlayHandle,
                nodeName = firstPlayNodeName,
                thumbnail = thumbnail,
                index = 0,
                type = PlaylistItem.TYPE_PLAYING,
                size = node?.size ?: INVALID_SIZE,
                duration = node?.duration ?: 0,
            )
            playlistItems.add(playlistItem)
            playlistItemsMap[firstPlayHandle.toString()] = playlistItem
            postPlaylistItems()

            if (thumbnail != null && !thumbnail.exists()) {
                megaApi.getThumbnail(node, thumbnail.absolutePath, createThumbnailRequest)
            } else {
                postPlayingThumbnail()
            }
        }

        return true
    }

    /**
     * Setup transfer listener
     */
    private fun setupTransferListener() {
        getGlobalTransferUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .filter { it is Result.OnTransferTemporaryError && it.transfer != null }
            .subscribeBy(
                onNext = { event ->
                    val errorEvent = event as Result.OnTransferTemporaryError
                    errorEvent.transfer?.run {
                        onTransferTemporaryError(this, errorEvent.error)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeBy(onError = { Timber.e(it) })
                            .addTo(compositeDisposable)
                    }
                },
                onError = { Timber.e(it) }
            )
            .addTo(compositeDisposable)
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
            VIDEO_BROWSE_ADAPTER,
            FROM_CHAT,
            FILE_LINK_ADAPTER,
            PHOTO_SYNC_ADAPTER,
            -> {
                return oldType == type
            }
            FILE_BROWSER_ADAPTER,
            RUBBISH_BIN_ADAPTER,
            INBOX_ADAPTER,
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

    private fun filterByNodeName(name: String): Boolean {
        val mime = MimeTypeList.typeForName(name)

        return if (isAudioPlayer) {
            mime.isAudio && !mime.isAudioNotSupported
        } else {
            mime.isVideo && mime.isVideoReproducible && !mime.isVideoNotSupported
        }
    }

    private fun buildPlaylistFromOfflineNodes(intent: Intent, firstPlayHandle: Long) {
        val nodes = with(intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableArrayListExtra(INTENT_EXTRA_KEY_ARRAY_OFFLINE, MegaOffline::class.java)
            } else {
                @Suppress("DEPRECATION")
                getParcelableArrayListExtra(INTENT_EXTRA_KEY_ARRAY_OFFLINE)
            }
        } ?: return

        doBuildPlaylist(
            megaApi, nodes, firstPlayHandle,
            {
                val file = getOfflineFile(context, it)
                isFileAvailable(file) && file.isFile && filterByNodeName(it.name)
            },
            {
                mediaItemFromFile(getOfflineFile(context, it), it.handle)
            },
            {
                it.handle.toLong()
            },
            {
                it.name
            },
            {
                offlineThumbnailFileWrapper.getThumbnailFile(context, it)
            },
            {
                it.getSize(context)
            }
        )
    }

    private fun buildPlaylistFromParent(parent: MegaNode, intent: Intent, firstPlayHandle: Long) {
        val order = intent.getIntExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, ORDER_DEFAULT_ASC)
        val children = megaApi.getChildren(parent, order) ?: return
        buildPlaylistFromNodes(megaApi, children, firstPlayHandle)
    }

    private fun buildPlaylistForAudio(intent: Intent, firstPlayHandle: Long) {
        val order = intent.getIntExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, ORDER_DEFAULT_ASC)
        cancelToken = initNewSearch()
        buildPlaylistFromNodes(
            megaApi,
            megaApi.searchByType(cancelToken ?: return,
                order,
                FILE_TYPE_AUDIO,
                SEARCH_TARGET_ROOTNODE),
            firstPlayHandle
        )
    }

    private fun buildPlaylistForVideos(intent: Intent, firstPlayHandle: Long) {
        val order = intent.getIntExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, ORDER_DEFAULT_ASC)
        cancelToken = initNewSearch()
        buildPlaylistFromNodes(
            api = megaApi,
            nodes = megaApi.searchByType(cancelToken ?: return,
                order,
                FILE_TYPE_VIDEO,
                SEARCH_TARGET_ROOTNODE),
            firstPlayHandle = firstPlayHandle
        )
    }

    override fun initNewSearch(): MegaCancelToken {
        cancelSearch()
        return MegaCancelToken.createInstance()
    }

    override fun cancelSearch() {
        cancelToken?.cancel()
    }

    private fun buildPlaylistFromHandles(handles: List<Long>, firstPlayHandle: Long) {
        val nodes = ArrayList<MegaNode>()

        for (handle in handles) {
            val node = megaApi.getNodeByHandle(handle)
            if (node != null) {
                nodes.add(node)
            }
        }

        buildPlaylistFromNodes(megaApi, nodes, firstPlayHandle)
    }

    private fun buildPlaylistFromFiles(files: List<File>, firstPlayHandle: Long) {
        doBuildPlaylist(
            megaApi, files, firstPlayHandle,
            {
                it.isFile && filterByNodeName(it.name)
            },
            {
                mediaItemFromFile(it, it.name.hashCode().toString())
            },
            {
                it.name.hashCode().toLong()
            },
            {
                it.name
            },
            {
                null
            },
            {
                it.length()
            }
        )
    }

    private fun buildPlaylistFromNodes(
        api: MegaApiAndroid,
        nodes: List<MegaNode>,
        firstPlayHandle: Long,
        isFolderLink: Boolean = false,
    ) {
        doBuildPlaylist(
            api, nodes, firstPlayHandle,
            {
                it.isFile && filterByNodeName(it.name)
            },
            {
                val localPath = getLocalFile(it)
                if (isLocalFile(it, api, localPath)) {
                    mediaItemFromFile(File(localPath), it.handle.toString())
                } else {
                    val authorizedNode = if (isFolderLink) {
                        megaApiFolder.authorizeNode(it)
                    } else {
                        it
                    }

                    val url = api.httpServerGetLocalLink(authorizedNode)
                    if (url == null) {
                        null
                    } else {
                        MediaItem.Builder()
                            .setUri(Uri.parse(url))
                            .setMediaId(it.handle.toString())
                            .build()
                    }
                }
            },
            {
                it.handle
            },
            {
                it.name
            },
            {
                File(getThumbFolder(context), it.base64Handle.plus(JPG_EXTENSION))
            },
            {
                it.size
            }
        )
    }

    private fun <T> doBuildPlaylist(
        api: MegaApiAndroid,
        nodes: List<T>,
        firstPlayHandle: Long,
        validator: (T) -> Boolean,
        mapper: (T) -> MediaItem?,
        handleGetter: (T) -> Long,
        nameGetter: (T) -> String,
        thumbnailGetter: (T) -> File?,
        sizeGetter: (T) -> Long,
    ) {
        playlistItems.clear()
        playlistItemsMap.clear()

        val mediaItems = ArrayList<MediaItem>()
        var index = 0
        var firstPlayIndex = 0

        val nodesWithoutThumbnail = ArrayList<Pair<Long, File>>()

        for (node in nodes) {
            if (!validator(node)) {
                continue
            }

            val mediaItem = mapper(node) ?: continue
            mediaItems.add(mediaItem)

            val handle = handleGetter(node)
            val thumbnail = thumbnailGetter(node)

            if (handle == firstPlayHandle) {
                firstPlayIndex = index
            }

            val playlistItem =
                PlaylistItem(
                    nodeHandle = handle,
                    nodeName = nameGetter(node),
                    thumbnail = thumbnail,
                    index = index,
                    type = PlaylistItem.TYPE_NEXT,
                    size = sizeGetter(node),
                    duration = if (node is MegaNode) {
                        node.duration
                    } else {
                        0
                    }
                )
            playlistItems.add(playlistItem)
            playlistItemsMap[handle.toString()] = playlistItem

            if (thumbnail != null && !thumbnail.exists()) {
                nodesWithoutThumbnail.add(Pair(handle, thumbnail))
            }

            index++
        }

        if (mediaItems.isNotEmpty()) {
            playerSource.postValue(MediaPlaySources(mediaItems, firstPlayIndex, null))
            postPlaylistItems()
        }

        if (nodesWithoutThumbnail.isNotEmpty() && isOnline(context)) {
            compositeDisposable.add(
                Observable.fromIterable(nodesWithoutThumbnail)
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                        val node = api.getNodeByHandle(it.first)
                        if (node != null) {
                            api.getThumbnail(
                                node, it.second.absolutePath, createThumbnailRequest
                            )
                        }
                    }, logErr("AudioPlayerServiceViewModel createThumbnail"))
            )
        }
    }

    private fun mediaItemFromFile(file: File, handle: String): MediaItem {
        return MediaItem.Builder()
            .setUri(getUriForFile(context, file))
            .setMediaId(handle)
            .build()
    }

    private fun postPlayingThumbnail() {
        val thumbnail = playlistItemsMap[playingHandle.toString()]?.thumbnail ?: return
        playingThumbnail.postValue(thumbnail)
    }

    /**
     * Get playlist from playlistItems
     * @param isScroll whether scroll to the specific position
     */
    private fun postPlaylistItems(currentPosition: Long? = null, isScroll: Boolean = true) {
        Timber.d("postPlaylistItems")
        compositeDisposable.add(Completable.fromCallable {
            doPostPlaylistItems(currentPosition,
                isScroll)
        }
            .subscribeOn(Schedulers.single())
            .subscribe(IGNORE, logErr("AudioPlayerServiceViewModel postPlaylistItems")))
    }

    /**
     * Get playlist from playlistItems
     * @param isScroll whether scroll to the specific position
     */
    private fun doPostPlaylistItems(currentPosition: Long? = null, isScroll: Boolean = true) {
        Timber.d("doPostPlaylistItems ${playlistItems.size} items")
        if (playlistItems.isEmpty()) {
            return
        }
        var playingIndex = 0
        for ((index, item) in playlistItems.withIndex()) {
            if (item.nodeHandle == playingHandle) {
                playingIndex = index
                playingPosition = playingIndex
                break
            }
        }
        val order = shuffleOrder

        val items: ArrayList<PlaylistItem>
        if (shuffleEnabled && order.length == playlistItems.size) {
            items = ArrayList()

            items.add(playlistItems[playingIndex])

            var newPlayingIndex = 0
            var index = order.getPreviousIndex(playingIndex)
            while (index != C.INDEX_UNSET) {
                items.add(0, playlistItems[index])
                index = order.getPreviousIndex(index)
                newPlayingIndex++
            }

            index = order.getNextIndex(playingIndex)
            while (index != C.INDEX_UNSET) {
                items.add(playlistItems[index])
                index = order.getNextIndex(index)
            }

            playingIndex = newPlayingIndex
        } else {
            items = ArrayList(playlistItems)
        }

        val searchQuery = playlistSearchQuery
        if (!TextUtil.isTextEmpty(searchQuery)) {
            filterPlaylistItems(items, searchQuery ?: return)
            return
        }
        for ((index, item) in items.withIndex()) {
            val type = when {
                index < playingIndex -> PlaylistItem.TYPE_PREVIOUS
                playingIndex == index -> PlaylistItem.TYPE_PLAYING
                else -> PlaylistItem.TYPE_NEXT
            }
            items[index] =
                item.finalizeItem(
                    index = index,
                    type = type,
                    isSelected = item.isSelected,
                    duration = item.duration,
                    currentPosition = if (playingIndex == index) {
                        currentPosition ?: item.currentPosition
                    } else {
                        item.currentPosition
                    }
                )
        }

        val hasPrevious = playingIndex > 0

        var scrollPosition = playingIndex

        if (hasPrevious) {
            items[0].headerIsVisible = true
        }

        items[playingIndex].headerIsVisible = true
        Timber.d("doPostPlaylistItems post ${items.size} items")
        if (!isScroll) {
            scrollPosition = -1
        }
        playlist.postValue(Pair(items, scrollPosition))
    }

    override fun setCurrentPosition(currentPosition: Long) {
        postPlaylistItems(currentPosition, false)
    }

    private fun filterPlaylistItems(items: List<PlaylistItem>, filter: String) {
        if (items.isEmpty()) return

        val filteredItems = ArrayList<PlaylistItem>()
        items.forEachIndexed { index, item ->
            if (item.nodeName.contains(filter, true)) {
                // Filter only affects displayed playlist, it doesn't affect what
                // ExoPlayer is playing, so we still need use the index before filter.
                filteredItems.add(item.finalizeItem(index, PlaylistItem.TYPE_PREVIOUS))
            }
        }

        playlist.postValue(Pair(filteredItems, 0))
    }

    override fun searchQueryUpdate(newText: String?) {
        playlistSearchQuery = newText
        postPlaylistItems()
    }

    override fun getCurrentIntent() = currentIntent

    override fun getCurrentPlayingHandle() = playingHandle

    override fun setCurrentPlayingHandle(handle: Long) {
        playingHandle = handle
        postPlaylistItems()
        postPlayingThumbnail()
    }

    override fun getPlaylistItem(handle: String?): PlaylistItem? {
        if (handle == null) {
            return null
        }
        return playlistItemsMap[handle]
    }

    override fun getPlayingThumbnail() = playingThumbnail

    override fun playerSourceUpdate() = playerSource.asFlow()

    override fun mediaItemToRemoveUpdate() = mediaItemToRemove.asFlow()

    override fun nodeNameUpdate() = nodeNameUpdate.asFlow()

    override fun retryUpdate() = retry.asFlow()

    override fun playlistUpdate() = playlist.asFlow()

    override fun mediaPlaybackUpdate() = mediaPlayback.asFlow()

    override fun errorUpdate() = error.asFlow()

    override fun playlistTitleUpdate() = playlistTitle.asFlow()

    override fun itemsSelectedCountUpdate() = itemsSelectedCount.asFlow()

    override fun actionModeUpdate() = actionMode.asFlow()

    override fun removeItem(handle: Long) {
        initPlayerSourceChanged()
        removeSingleItem(handle)
        if (playlistItems.isEmpty()) {
            playlist.value = Pair(emptyList(), 0)
            error.value = MegaError.API_ENOENT
        } else {
            resetRetryState()
            postPlaylistItems()
        }
    }

    private fun removeSingleItem(handle: Long) {
        mediaItemToRemove.value = playlistItems.indexOfFirst { (nodeHandle) ->
            nodeHandle == handle
        }
        playlistItems.removeIf { (nodeHandle) ->
            nodeHandle == handle
        }
        playSourceChanged.removeIf { mediaItem ->
            mediaItem.mediaId.toLong() == handle
        }
    }

    override fun removeItems() {
        if (itemsSelectedMap.isNotEmpty()) {
            initPlayerSourceChanged()
            itemsSelectedMap.forEach {
                removeSingleItem(it.value.nodeHandle)
            }
            itemsSelectedMap.clear()
            if (playlistItems.isNotEmpty()) {
                postPlaylistItems()
            } else {
                playlist.value = Pair(emptyList(), 0)
                error.value = MegaError.API_ENOENT
            }
            itemsSelectedCount.value = itemsSelectedMap.size
            actionMode.value = false
        }
    }

    override fun itemSelected(handle: Long) {
        playlistItems.forEach {
            if (it.nodeHandle == handle) {
                it.isSelected = !it.isSelected
                if (it.isSelected) {
                    itemsSelectedMap[handle] = it
                } else {
                    itemsSelectedMap.remove(handle)
                }
                itemsSelectedCount.value = itemsSelectedMap.size
                // Refresh the playlist
                postPlaylistItems(isScroll = false)
            }
        }
    }

    override fun clearSelections() {
        playlistItems.forEach {
            it.isSelected = false
            itemsSelectedMap.clear()
            actionMode.value = false
            postPlaylistItems()
        }
    }

    override fun setActionMode(isActionMode: Boolean) {
        actionMode.value = isActionMode
        if (isActionMode) {
            postPlaylistItems(isScroll = false)
        }
    }

    override fun resetRetryState() {
        playerRetry = 0
        retry.value = true
    }

    override fun updateItemName(handle: Long, newName: String) {
        for ((index, item) in playlistItems.withIndex()) {
            if (item.nodeHandle == handle) {
                val newItem = playlistItems[index].updateNodeName(newName)
                playlistItems[index] = newItem
                playlistItemsMap[handle.toString()] = newItem
                postPlaylistItems()
                if (handle == playingHandle) {
                    nodeNameUpdate.postValue(newName)
                }
                return
            }
        }
    }

    override fun getPlaylistItems() = playlist.value?.first

    override fun isAudioPlayer() = isAudioPlayer
    override fun setAudioPlayer(isAudioPlayer: Boolean) {
        this.isAudioPlayer = isAudioPlayer
    }

    override fun backgroundPlayEnabled() = backgroundPlayEnabled

    override fun toggleBackgroundPlay(): Boolean {
        backgroundPlayEnabled = !backgroundPlayEnabled
        preferences.edit()
            .putBoolean(KEY_AUDIO_BACKGROUND_PLAY_ENABLED, backgroundPlayEnabled)
            .apply()

        return backgroundPlayEnabled
    }

    override fun shuffleEnabled(): Boolean {
        return shuffleEnabled
    }

    override fun getShuffleOrder() = shuffleOrder

    override fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled = enabled
        preferences.edit()
            .putBoolean(KEY_AUDIO_SHUFFLE_ENABLED, shuffleEnabled)
            .apply()

        postPlaylistItems()
    }

    override fun newShuffleOrder(): ShuffleOrder {
        shuffleOrder = ExposedShuffleOrder(playlistItems.size, this)
        return shuffleOrder
    }

    override fun audioRepeatToggleMode() = audioRepeatToggleMode

    override fun videoRepeatToggleMode() = videoRepeatToggleMode

    override fun setAudioRepeatMode(repeatToggleMode: RepeatToggleMode) {
        audioRepeatToggleMode = repeatToggleMode
        preferences.edit().putInt(KEY_AUDIO_REPEAT_MODE, repeatToggleMode.ordinal).apply()
    }

    override fun setVideoRepeatMode(repeatToggleMode: RepeatToggleMode) {
        videoRepeatToggleMode = repeatToggleMode
        preferences.edit().putInt(KEY_VIDEO_REPEAT_MODE, repeatToggleMode.ordinal).apply()
    }

    private fun convertToRepeatToggleMode(ordinal: Int): RepeatToggleMode =
        when (ordinal) {
            RepeatToggleMode.REPEAT_NONE.ordinal -> RepeatToggleMode.REPEAT_NONE
            RepeatToggleMode.REPEAT_ONE.ordinal -> RepeatToggleMode.REPEAT_ONE
            else -> RepeatToggleMode.REPEAT_ALL
        }

    override fun clear() {
        compositeDisposable.dispose()

        if (needStopStreamingServer) {
            megaApi.httpServerStop()
            megaApiFolder.httpServerStop()
        }
    }

    private fun getApi(type: Int) =
        if (type == FOLDER_LINK_ADAPTER && dbHandler.credentials == null) megaApiFolder else megaApi

    override fun swapItems(current: Int, target: Int) {
        playlistItems.run {
            Collections.swap(this, current, target)
            // Keep the index for swap items to keep the play order is correct
            val index = this[current].index
            this[current].index = this[target].index
            this[target].index = index
        }
        initPlayerSourceChanged()
        // Swap the items of play source
        Collections.swap(playSourceChanged, current, target)
    }

    override fun getIndexFromPlaylistItems(item: PlaylistItem): Int {
        return playlistItems.indexOfFirst {
            it.nodeName == item.nodeName
        }
    }

    override fun updatePlaySource() {
        val newPlayerSource = mutableListOf<MediaItem>()
        newPlayerSource.addAll(playSourceChanged)
        playerSource.value?.run {
            playerSource.value =
                copy(mediaItems = newPlayerSource, newIndexForCurrentItem = playingPosition)
            playSourceChanged.clear()
        }
    }

    override fun isPaused() = paused

    override fun getPlayingPosition(): Int {
        return playingPosition
    }

    override fun scrollToPlayingPosition() {
        postPlaylistItems(isScroll = true)
    }

    override fun isActionMode() = actionMode.value

    private fun initPlayerSourceChanged() {
        if (playSourceChanged.isEmpty()) {
            // Get the play source
            playerSource.value?.run {
                playSourceChanged.addAll(mediaItems)
            }
        }
    }

    override fun onShuffleChanged(newShuffle: ShuffleOrder) {
        shuffleOrder = newShuffle
    }

    private fun onTransferTemporaryError(transfer: MegaTransfer, e: MegaError): Completable =
        Completable.fromAction {
            if (transfer.nodeHandle != playingHandle) {
                return@fromAction
            }

            if ((e.errorCode == MegaError.API_EOVERQUOTA && !transfer.isForeignOverquota && e.value != 0L)
                || e.errorCode == MegaError.API_EBLOCKED
            ) {
                error.value = e.errorCode
            }
        }

    companion object {
        private const val MAX_RETRY = 6

        /**
         * Clear saved audio player settings.
         *
         * @param context Android context
         */
        @JvmStatic
        fun clearSettings(context: Context) {
            context.defaultSharedPreferences.edit()
                .remove(KEY_AUDIO_BACKGROUND_PLAY_ENABLED)
                .remove(KEY_AUDIO_SHUFFLE_ENABLED)
                .remove(KEY_AUDIO_REPEAT_MODE)
                .apply()
        }
    }
}
