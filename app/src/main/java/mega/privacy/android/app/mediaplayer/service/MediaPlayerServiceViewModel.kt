package mega.privacy.android.app.mediaplayer.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ShuffleOrder
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.MegaRequestFinishListener
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.ContactUtil.getMegaUserNameDB
import mega.privacy.android.app.utils.FileUtil.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.MegaNodeUtil.isInRootLinksLevel
import mega.privacy.android.app.utils.MegaNodeUtil.setupStreamingServer
import mega.privacy.android.app.utils.OfflineUtils.*
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder
import mega.privacy.android.app.utils.Util.isOnline
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.*
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * A class containing audio player service logic, because using ViewModel in Service
 * is not the standard scenario, so this class is actually not a subclass of ViewModel.
 */
class MediaPlayerServiceViewModel(
    private val context: Context,
    private val megaApi: MegaApiAndroid,
    private val megaApiFolder: MegaApiAndroid,
    private val dbHandler: DatabaseHandler,
) : ExposedShuffleOrder.ShuffleChangeListener, MegaTransferListenerInterface {
    private val compositeDisposable = CompositeDisposable()

    private val downloadLocationDefaultPath = getDownloadLocation()

    private val preferences = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)

    private var backgroundPlayEnabled = preferences.getBoolean(KEY_BACKGROUND_PLAY_ENABLED, true)
    private var shuffleEnabled = preferences.getBoolean(KEY_SHUFFLE_ENABLED, false)
    private var repeatMode = preferences.getInt(KEY_REPEAT_MODE, Player.REPEAT_MODE_OFF)

    private val createThumbnailFinished = PublishSubject.create<Boolean>()
    private val createThumbnailRequest = MegaRequestFinishListener({
        createThumbnailFinished.onNext(true)

        if (it.nodeHandle == playingHandle) {
            postPlayingThumbnail()
        }
    })

    private val _playerSource = MutableLiveData<Triple<List<MediaItem>, Int, String?>>()
    val playerSource: LiveData<Triple<List<MediaItem>, Int, String?>> = _playerSource

    private val _mediaItemToRemove = MutableLiveData<Int>()
    val mediaItemToRemove: LiveData<Int> = _mediaItemToRemove

    private val _nodeNameUpdate = MutableLiveData<String>()
    val nodeNameUpdate: LiveData<String> = _nodeNameUpdate

    private val _playingThumbnail = MutableLiveData<File>()
    val playingThumbnail: LiveData<File> = _playingThumbnail

    private val _playlist = MutableLiveData<Triple<List<PlaylistItem>, Int, String>>()
    val playlist: LiveData<Triple<List<PlaylistItem>, Int, String>> = _playlist

    private val _retry = MutableLiveData<Boolean>()
    val retry: LiveData<Boolean> = _retry

    private val _error = MutableLiveData<Int>()
    val error: LiveData<Int> = _error

    var currentIntent: Intent? = null
        private set
    var playlistTitle = ""

    private val playlistItems = ArrayList<PlaylistItem>()
    private val playlistItemsMap = HashMap<String, PlaylistItem>()

    var playlistSearchQuery: String? = null
        set(value) {
            field = value
            postPlaylistItems()
        }

    var shuffleOrder: ShuffleOrder = ExposedShuffleOrder(0, this)

    var playingHandle = INVALID_HANDLE
        set(value) {
            field = value
            postPlaylistItems()
            postPlayingThumbnail()
        }

    var paused = false
        set(value) {
            field = value
            postPlaylistItems()
        }

    var audioPlayer = false
        private set

    private var playerRetry = 0

    init {
        compositeDisposable.add(
            createThumbnailFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe(
                    { postPlaylistItems() },
                    logErr("AudioPlayerServiceViewModel creatingThumbnailFinished")
                )
        )

        megaApi.addTransferListener(this)
    }

    /**
     * Build player source from start intent.
     *
     * @param intent intent received from onStartCommand
     * @return if there is no error
     */
    fun buildPlayerSource(intent: Intent?): Boolean {
        if (intent == null || !intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)) {
            _retry.value = false
            return false
        }

        val type = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val uri = intent.data

        if (type == INVALID_VALUE || uri == null) {
            _retry.value = false
            return false
        }

        val samePlaylist = isSamePlaylist(type, intent)
        currentIntent = intent

        val firstPlayHandle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
        if (firstPlayHandle == INVALID_HANDLE) {
            _retry.value = false
            return false
        }

        val firstPlayNodeName = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME)
        if (firstPlayNodeName == null) {
            _retry.value = false
            return false
        }

        playerRetry = 0
        audioPlayer = MimeTypeList.typeForName(firstPlayNodeName).isAudio

        var displayNodeNameFirst = type != OFFLINE_ADAPTER || !audioPlayer
        if (samePlaylist && firstPlayHandle == playingHandle) {
            // if we are already playing this music, then the metadata is already
            // in LiveData (_metadata of AudioPlayerService), we don't need (and shouldn't)
            // emit node name.
            displayNodeNameFirst = false
        }

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(firstPlayHandle.toString())
            .build()
        _playerSource.value = Triple(
            listOf(mediaItem),
            // we will emit a single item list at first, and the current playing item
            // will always be at index 0 in that single item list.
            if (samePlaylist && firstPlayHandle == playingHandle) 0 else INVALID_VALUE,
            if (displayNodeNameFirst) firstPlayNodeName else null
        )

        if (intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)) {
            if (type != OFFLINE_ADAPTER && type != ZIP_ADAPTER) {
                setupStreamingServer(getApi(type), context)
            }

            compositeDisposable.add(
                Completable
                    .fromCallable(Callable {
                        when (type) {
                            OFFLINE_ADAPTER -> {
                                playlistTitle = getOfflineFolderName(context, firstPlayHandle)

                                buildPlaylistFromOfflineNodes(intent, firstPlayHandle)
                            }
                            AUDIO_BROWSE_ADAPTER -> {
                                playlistTitle = getString(R.string.upload_to_audio)

                                buildPlaylistForAudio(intent, firstPlayHandle)
                            }
                            VIDEO_BROWSE_ADAPTER -> {
                                playlistTitle = getString(R.string.sortby_type_video_first)

                                buildPlaylistForVideos(intent, firstPlayHandle)
                            }
                            FILE_BROWSER_ADAPTER,
                            RUBBISH_BIN_ADAPTER,
                            INBOX_ADAPTER,
                            LINKS_ADAPTER,
                            INCOMING_SHARES_ADAPTER,
                            OUTGOING_SHARES_ADAPTER,
                            CONTACT_FILE_ADAPTER,
                            PHOTO_SYNC_ADAPTER -> {
                                val parentHandle = intent.getLongExtra(
                                    INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                                    INVALID_HANDLE
                                )
                                val order = intent.getIntExtra(
                                    INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                                    ORDER_DEFAULT_ASC
                                )

                                if (isInRootLinksLevel(type, parentHandle)) {
                                    playlistTitle = getString(R.string.tab_links_shares)

                                    buildPlaylistFromNodes(
                                        megaApi, megaApi.getPublicLinks(order), firstPlayHandle
                                    )
                                    return@Callable
                                }

                                if (type == INCOMING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE) {
                                    playlistTitle = getString(R.string.tab_incoming_shares)

                                    buildPlaylistFromNodes(
                                        megaApi, megaApi.getInShares(order), firstPlayHandle
                                    )
                                    return@Callable
                                }

                                if (type == OUTGOING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE) {
                                    playlistTitle = getString(R.string.tab_outgoing_shares)

                                    val nodes = ArrayList<MegaNode>()
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

                                    playlistTitle =
                                        getString(R.string.title_incoming_shares_with_explorer) +
                                                getMegaUserNameDB(contact)

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

                                playlistTitle = if (parentHandle == INVALID_HANDLE) {
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

                                buildPlaylistFromParent(parent, intent, firstPlayHandle)
                            }
                            RECENTS_ADAPTER, RECENTS_BUCKET_ADAPTER -> {
                                playlistTitle = getString(R.string.section_recents)

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

                                playlistTitle = parent.name

                                buildPlaylistFromNodes(
                                    getApi(type), megaApiFolder.getChildren(parent, order),
                                    firstPlayHandle
                                )
                            }
                            ZIP_ADAPTER -> {
                                val zipPath =
                                    intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)
                                        ?: return@Callable

                                playlistTitle = File(zipPath).parentFile?.name ?: ""

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
                    .subscribeOn(Schedulers.io())
                    .subscribe(IGNORE, logErr("AudioPlayerServiceViewModel buildPlayerSource"))
            )
        } else {
            playlistItems.clear()
            playlistItemsMap.clear()

            val node = megaApi.getNodeByHandle(firstPlayHandle)
            val thumbnail = when {
                type == OFFLINE_ADAPTER -> {
                    getThumbnailFile(context, firstPlayHandle.toString())
                }
                node == null -> {
                    null
                }
                else -> {
                    File(getThumbFolder(context), node.base64Handle.plus(JPG_EXTENSION))
                }
            }

            val playlistItem = PlaylistItem(
                firstPlayHandle, firstPlayNodeName, thumbnail, 0, PlaylistItem.TYPE_PLAYING
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
     * Handle player error.
     */
    fun onPlayerError() {
        playerRetry++
        _retry.value = playerRetry <= MAX_RETRY
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
            FILE_LINK_ADAPTER -> {
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
            PHOTO_SYNC_ADAPTER -> {
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

        return if (audioPlayer) {
            mime.isAudio && !mime.isAudioNotSupported
        } else {
            mime.isVideo && mime.isVideoReproducible && !mime.isVideoNotSupported
        }
    }

    private fun buildPlaylistFromOfflineNodes(intent: Intent, firstPlayHandle: Long) {
        val nodes = intent.getParcelableArrayListExtra<MegaOffline>(INTENT_EXTRA_KEY_ARRAY_OFFLINE)
            ?: return

        doBuildPlaylist(
            megaApi, nodes, firstPlayHandle,
            {
                isFileAvailable(getOfflineFile(context, it)) && filterByNodeName(it.name)
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
                getThumbnailFile(context, it)
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
        buildPlaylistFromNodes(
            megaApi, megaApi.searchByType(order, FILE_TYPE_AUDIO, SEARCH_TARGET_ROOTNODE),
            firstPlayHandle
        )
    }

    private fun buildPlaylistForVideos(intent: Intent, firstPlayHandle: Long) {
        val order = intent.getIntExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, ORDER_DEFAULT_ASC)
        buildPlaylistFromNodes(
            megaApi, megaApi.searchByType(order, FILE_TYPE_VIDEO, SEARCH_TARGET_ROOTNODE),
            firstPlayHandle
        )
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
            }
        )
    }

    private fun buildPlaylistFromNodes(
        api: MegaApiAndroid, nodes: List<MegaNode>, firstPlayHandle: Long
    ) {
        doBuildPlaylist(
            api, nodes, firstPlayHandle,
            {
                filterByNodeName(it.name)
            },
            {
                var isOnMegaDownloads = false
                val f = File(downloadLocationDefaultPath, it.name)
                if (f.exists() && f.length() == it.size) {
                    isOnMegaDownloads = true
                }

                val localPath = getLocalFile(context, it.name, it.size)
                val nodeFingerPrint = api.getFingerprint(it)
                val localPathFingerPrint = api.getFingerprint(localPath)

                if (localPath != null
                    && (isOnMegaDownloads || nodeFingerPrint != null
                            && nodeFingerPrint == localPathFingerPrint)
                ) {
                    mediaItemFromFile(File(localPath), it.handle.toString())
                } else if (dbHandler.credentials != null) {
                    MediaItem.Builder()
                        .setUri(Uri.parse(api.httpServerGetLocalLink(it)))
                        .setMediaId(it.handle.toString())
                        .build()
                } else {
                    null
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
                PlaylistItem(handle, nameGetter(node), thumbnail, index, PlaylistItem.TYPE_NEXT)
            playlistItems.add(playlistItem)
            playlistItemsMap[handle.toString()] = playlistItem

            if (thumbnail != null && !thumbnail.exists()) {
                nodesWithoutThumbnail.add(Pair(handle, thumbnail))
            }

            index++
        }

        if (mediaItems.isNotEmpty()) {
            _playerSource.postValue(Triple(mediaItems, firstPlayIndex, null))

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
        _playingThumbnail.postValue(thumbnail)
    }

    private fun postPlaylistItems() {
        logDebug("postPlaylistItems")

        compositeDisposable.add(Completable.fromCallable { doPostPlaylistItems() }
            .subscribeOn(Schedulers.single())
            .subscribe(IGNORE, logErr("AudioPlayerServiceViewModel postPlaylistItems")))
    }

    private fun doPostPlaylistItems() {
        logDebug("doPostPlaylistItems ${playlistItems.size} items")

        if (playlistItems.isEmpty()) {
            return
        }

        var playingIndex = 0
        for ((index, item) in playlistItems.withIndex()) {
            if (item.nodeHandle == playingHandle) {
                playingIndex = index
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
            filterPlaylistItems(items, searchQuery!!)
            return
        }

        for ((index, item) in items.withIndex()) {
            val type = when {
                index < playingIndex -> PlaylistItem.TYPE_PREVIOUS
                playingIndex == index -> PlaylistItem.TYPE_PLAYING
                else -> PlaylistItem.TYPE_NEXT
            }
            items[index] = item.finalizeThumbnailAndType(type)
        }

        val hasPrevious = playingIndex > 0
        val hasNext = playingIndex < playlistItems.size - 1

        var offset = 0
        var scrollPosition = playingIndex

        if (hasPrevious) {
            items.add(0, PlaylistItem.headerItem(PlaylistItem.TYPE_PREVIOUS_HEADER))
            offset++
            scrollPosition++
        }

        items.add(
            playingIndex + offset,
            PlaylistItem.headerItem(PlaylistItem.TYPE_PLAYING_HEADER, paused)
        )
        offset += 2

        if (hasNext) {
            items.add(
                playingIndex + offset,
                PlaylistItem.headerItem(PlaylistItem.TYPE_NEXT_HEADER)
            )
        }

        logDebug("doPostPlaylistItems post ${items.size} items")
        _playlist.postValue(Triple(items, scrollPosition, playlistTitle))
    }

    private fun filterPlaylistItems(items: List<PlaylistItem>, filter: String) {
        val filteredItems = ArrayList<PlaylistItem>()

        for (item in items) {
            if (item.nodeName.toLowerCase(Locale.ROOT).contains(filter)) {
                filteredItems.add(item.finalizeThumbnailAndType(PlaylistItem.TYPE_PREVIOUS))
            }
        }

        _playlist.postValue(Triple(filteredItems, 0, playlistTitle))
    }

    fun getPlaylistItem(handle: String?): PlaylistItem? {
        if (handle == null) {
            return null
        }
        return playlistItemsMap[handle]
    }

    fun removeItem(handle: Long) {
        for ((index, item) in playlistItems.withIndex()) {
            if (item.nodeHandle == handle) {
                playlistItems.removeAt(index)
                _mediaItemToRemove.value = index
                if (playlistItems.isEmpty()) {
                    _playlist.value = Triple(emptyList(), 0, playlistTitle)
                } else {
                    postPlaylistItems()
                }
                return
            }
        }
    }

    fun updateItemName(handle: Long, newName: String) {
        for ((index, item) in playlistItems.withIndex()) {
            if (item.nodeHandle == handle) {
                val newItem = playlistItems[index].updateNodeName(newName)
                playlistItems[index] = newItem
                playlistItemsMap[handle.toString()] = newItem
                postPlaylistItems()
                if (handle == playingHandle) {
                    _nodeNameUpdate.postValue(newName)
                }
                return
            }
        }
    }

    fun backgroundPlayEnabled(): Boolean {
        return backgroundPlayEnabled
    }

    fun toggleBackgroundPlay(): Boolean {
        backgroundPlayEnabled = !backgroundPlayEnabled
        preferences.edit()
            .putBoolean(KEY_BACKGROUND_PLAY_ENABLED, backgroundPlayEnabled)
            .apply()

        return backgroundPlayEnabled
    }

    fun shuffleEnabled(): Boolean {
        return shuffleEnabled
    }

    fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled = enabled
        preferences.edit()
            .putBoolean(KEY_SHUFFLE_ENABLED, shuffleEnabled)
            .apply()

        postPlaylistItems()
    }

    fun newShuffleOrder(): ShuffleOrder {
        shuffleOrder = ExposedShuffleOrder(playlistItems.size, this)
        return shuffleOrder
    }

    fun repeatMode(): Int {
        return repeatMode
    }

    fun setRepeatMode(repeatMode: Int) {
        this.repeatMode = repeatMode
        preferences.edit()
            .putInt(KEY_REPEAT_MODE, repeatMode)
            .apply()
    }

    /**
     * Clear the state and flying task of this class, should be called in onDestroy.
     */
    fun clear() {
        compositeDisposable.dispose()
        megaApi.httpServerStop()
        megaApiFolder.httpServerStop()

        megaApi.removeTransferListener(this)
    }

    private fun getApi(type: Int) =
        if (type == FOLDER_LINK_ADAPTER && dbHandler.credentials == null) megaApiFolder else megaApi

    override fun onShuffleChanged(newShuffle: ShuffleOrder) {
        shuffleOrder = newShuffle

        postPlaylistItems()
    }

    companion object {
        private const val SETTINGS_FILE = "audio_player_settings"
        private const val KEY_BACKGROUND_PLAY_ENABLED = "background_play_enabled"
        private const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
        private const val KEY_REPEAT_MODE = "repeat_mode"

        private const val MAX_RETRY = 6

        /**
         * Clear saved audio player settings.
         *
         * @param context Android context
         */
        @JvmStatic
        fun clearSettings(context: Context) {
            context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }

    override fun onTransferStart(api: MegaApiJava, transfer: MegaTransfer) {
    }

    override fun onTransferFinish(api: MegaApiJava, transfer: MegaTransfer, e: MegaError) {
    }

    override fun onTransferUpdate(api: MegaApiJava, transfer: MegaTransfer) {
    }

    override fun onTransferTemporaryError(api: MegaApiJava, transfer: MegaTransfer, e: MegaError) {
        if ((e.errorCode == MegaError.API_EOVERQUOTA && e.value != 0L) || e.errorCode == MegaError.API_EBLOCKED) {
            _error.value = e.errorCode
        }
    }

    override fun onTransferData(api: MegaApiJava, transfer: MegaTransfer, buffer: ByteArray) = false
}
