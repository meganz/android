package mega.privacy.android.app.audioplayer.service

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
import mega.privacy.android.app.audioplayer.playlist.PlaylistItem
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.*
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.OfflineUtils.getThumbnailFile
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder
import mega.privacy.android.app.utils.Util.isOnline
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AudioPlayerServiceViewModel(
    private val context: Context,
    private val megaApi: MegaApiAndroid,
    private val dbHandler: DatabaseHandler,
) : ExposedShuffleOrder.ShuffleChangeListener {
    private val compositeDisposable = CompositeDisposable()

    private val downloadLocationDefaultPath = getDownloadLocation()

    private val preferences = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)

    private var backgroundPlayEnabled = preferences.getBoolean(KEY_BACKGROUND_PLAY_ENABLED, true)
    private var shuffleEnabled = preferences.getBoolean(KEY_SHUFFLE_ENABLED, false)
    private var repeatMode = preferences.getInt(KEY_REPEAT_MODE, Player.REPEAT_MODE_OFF)

    private val createThumbnailFinished = PublishSubject.create<Boolean>()
    private val createThumbnailRequest = object : BaseListener(context) {
        override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
            if (e.errorCode == MegaError.API_OK) {
                createThumbnailFinished.onNext(true)
            }
        }
    }

    private val _playerSource = MutableLiveData<Triple<List<MediaItem>, Int, String?>>()
    val playerSource: LiveData<Triple<List<MediaItem>, Int, String?>> = _playerSource

    private val _mediaItemToRemove = MutableLiveData<Int>()
    val mediaItemToRemove: LiveData<Int> = _mediaItemToRemove

    private val _nodeNameUpdate = MutableLiveData<String>()
    val nodeNameUpdate: LiveData<String> = _nodeNameUpdate

    private val _playlist = MutableLiveData<Triple<List<PlaylistItem>, Int, String>>()
    val playlist: LiveData<Triple<List<PlaylistItem>, Int, String>> = _playlist

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
        }

    var paused = false
        set(value) {
            field = value
            postPlaylistItems()
        }

    init {
        compositeDisposable.add(
            createThumbnailFinished.throttleLatest(1, TimeUnit.SECONDS, true)
                .subscribe(
                    { postPlaylistItems() },
                    logErr("AudioPlayerServiceViewModel creatingThumbnailFinished")
                )
        )
    }

    fun buildPlayerSource(intent: Intent?) {
        if (intent == null || !intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)) {
            return
        }

        val type = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val uri = intent.data

        if (type == INVALID_VALUE || uri == null) {
            return
        }

        val samePlaylist = isSamePlaylist(type, intent)
        currentIntent = intent

        var displayNodeNameFirst = true
        var firstPlayNodeName = ""
        when (type) {
            OFFLINE_ADAPTER -> {
                val path = intent.getStringExtra(INTENT_EXTRA_KEY_PATH) ?: return
                displayNodeNameFirst = false
                firstPlayNodeName = File(path).name
            }
            AUDIO_SEARCH_ADAPTER, AUDIO_BROWSE_ADAPTER -> {
                val handle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
                val node = megaApi.getNodeByHandle(handle) ?: return
                firstPlayNodeName = node.name
            }
            else -> {
                return
            }
        }

        val firstPlayHandle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)

        if (samePlaylist && firstPlayHandle == playingHandle) {
            // if we are already playing this music, then the metadata is already
            // in LiveData (_metadata of AudioPlayerService), we don't need (and can't)
            // emit node name.
            displayNodeNameFirst = false
        }

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(firstPlayHandle.toString())
            .build()
        _playerSource.value = Triple(
            listOf(mediaItem),
            if (samePlaylist && firstPlayHandle == playingHandle) 0 else INVALID_VALUE,
            if (displayNodeNameFirst) firstPlayNodeName else null
        )

        if (intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)) {
            compositeDisposable.add(Completable
                .fromCallable {
                    when (type) {
                        OFFLINE_ADAPTER -> {
                            playlistTitle = getOfflineFolderName(firstPlayHandle)

                            buildPlaylistFromOfflineNodes(intent, firstPlayHandle)
                        }
                        AUDIO_BROWSE_ADAPTER -> {
                            playlistTitle = context.getString(R.string.upload_to_audio)

                            buildPlaylistForAudio(intent, firstPlayHandle)
                        }
                    }
                }
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("AudioPlayerServiceViewModel buildPlayerSource")))
        }
    }

    private fun isSamePlaylist(type: Int, intent: Intent): Boolean {
        val oldIntent = currentIntent ?: return false

        when (type) {
            OFFLINE_ADAPTER -> {
                val oldDir = oldIntent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)
                    ?: return false
                val newDir =
                    intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY) ?: return false
                return oldDir == newDir
            }
            AUDIO_SEARCH_ADAPTER -> {
                val oldHandles = oldIntent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)
                    ?: return false
                val newHandles =
                    intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH) ?: return false
                return oldHandles.contentEquals(newHandles)
            }
            AUDIO_BROWSE_ADAPTER -> {
                return true
            }
            else -> {
                return false
            }
        }
    }

    private fun buildPlaylistFromOfflineNodes(intent: Intent, firstPlayHandle: Long) {
        val nodes = intent.getParcelableArrayListExtra<MegaOffline>(INTENT_EXTRA_KEY_ARRAY_OFFLINE)
            ?: return

        buildPlaylistFromNodes(
            nodes, firstPlayHandle,
            {
                isFileAvailable(getOfflineFile(context, it))
                        && MimeTypeList.typeForName(it.name).isAudio
                        && !MimeTypeList.typeForName(it.name).isAudioNotSupported
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

    private fun getOfflineFolderName(handle: Long): String {
        val node = dbHandler.findByHandle(handle) ?: return ""
        val file = getOfflineFile(context, node)
        if (!file.exists()) {
            return ""
        }

        val parentName = file.parentFile?.name ?: return ""
        val grandParentName = file.parentFile?.parentFile?.name
        return when {
            grandParentName != null
                    && grandParentName + File.separator + parentName == OfflineUtils.OFFLINE_INBOX_DIR ->
                context.getString(R.string.section_saved_for_offline_new)
            parentName == OfflineUtils.OFFLINE_DIR ->
                context.getString(R.string.section_saved_for_offline_new)
            else -> parentName
        }
    }

    private fun buildPlaylistFromHandles(intent: Intent, firstPlayHandle: Long) {
        val handles = intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH) ?: return
        buildPlaylistFromHandles(handles.toList(), firstPlayHandle)
    }

    private fun buildPlaylistForAudio(intent: Intent, firstPlayHandle: Long) {
        val order = intent.getIntExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, ORDER_DEFAULT_ASC)
        buildPlaylistFromNodes(
            megaApi.searchByType(order, FILE_TYPE_AUDIO, SEARCH_TARGET_ROOTNODE), firstPlayHandle
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

        buildPlaylistFromNodes(nodes, firstPlayHandle)
    }

    private fun buildPlaylistFromNodes(nodes: List<MegaNode>, firstPlayHandle: Long) {
        buildPlaylistFromNodes(
            nodes, firstPlayHandle,
            {
                MimeTypeList.typeForName(it.name).isAudio
                        && !MimeTypeList.typeForName(it.name).isAudioNotSupported
            },
            {
                var isOnMegaDownloads = false
                val f = File(downloadLocationDefaultPath, it.name)
                if (f.exists() && f.length() == it.size) {
                    isOnMegaDownloads = true
                }

                val localPath = getLocalFile(context, it.name, it.size)
                val nodeFingerPrint = megaApi.getFingerprint(it)
                val localPathFingerPrint = megaApi.getFingerprint(localPath)

                if (localPath != null
                    && (isOnMegaDownloads || nodeFingerPrint != null
                            && nodeFingerPrint == localPathFingerPrint)
                ) {
                    mediaItemFromFile(File(localPath), it.handle.toString())
                } else if (dbHandler.credentials != null) {
                    MediaItem.Builder()
                        .setUri(Uri.parse(megaApi.httpServerGetLocalLink(it)))
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

    private fun <T> buildPlaylistFromNodes(
        nodes: List<T>,
        firstPlayHandle: Long,
        validator: (T) -> Boolean,
        mapper: (T) -> MediaItem?,
        handleGetter: (T) -> Long,
        nameGetter: (T) -> String,
        thumbnailGetter: (T) -> File,
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

            if (!thumbnail.exists()) {
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
                        val node = megaApi.getNodeByHandle(it.first)
                        if (node != null) {
                            megaApi.getThumbnail(
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

    private fun postPlaylistItems() {
        compositeDisposable.add(Completable.fromCallable { doPostPlaylistItems() }
            .subscribeOn(Schedulers.single())
            .subscribe(IGNORE, logErr("AudioPlayerServiceViewModel postPlaylistItems")))
    }

    private fun doPostPlaylistItems() {
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
            items.add(0, PlaylistItem.headerItem(context, PlaylistItem.TYPE_PREVIOUS_HEADER))
            offset++
            scrollPosition++
        }

        items.add(
            playingIndex + offset,
            PlaylistItem.headerItem(context, PlaylistItem.TYPE_PLAYING_HEADER, paused)
        )
        offset += 2

        if (hasNext) {
            items.add(
                playingIndex + offset,
                PlaylistItem.headerItem(context, PlaylistItem.TYPE_NEXT_HEADER)
            )
        }

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

    fun toggleBackgroundPlay() {
        backgroundPlayEnabled = !backgroundPlayEnabled
        preferences.edit()
            .putBoolean(KEY_BACKGROUND_PLAY_ENABLED, backgroundPlayEnabled)
            .apply()
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

    fun clear() {
        compositeDisposable.dispose()
    }

    companion object {
        private const val SETTINGS_FILE = "audio_player_settings"
        private const val KEY_BACKGROUND_PLAY_ENABLED = "background_play_enabled"
        private const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
        private const val KEY_REPEAT_MODE = "repeat_mode"

        fun clearSettings(context: Context) {
            context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }

    override fun onShuffleChanged(newShuffle: ShuffleOrder) {
        shuffleOrder = newShuffle

        postPlaylistItems()
    }
}
