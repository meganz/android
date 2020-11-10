package mega.privacy.android.app.audioplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.*
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaNode
import java.io.File

class AudioPlayerViewModel(
    private val context: Context,
    private val megaApi: MegaApiAndroid,
    private val dbHandler: DatabaseHandler,
) {
    private val compositeDisposable = CompositeDisposable()

    private val downloadLocationDefaultPath = getDownloadLocation()

    private val preferences = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)

    private var backgroundPlayEnabled = preferences.getBoolean(KEY_BACKGROUND_PLAY_ENABLED, true)
    private var shuffleEnabled = preferences.getBoolean(KEY_SHUFFLE_ENABLED, false)
    private var repeatMode = preferences.getInt(KEY_REPEAT_MODE, Player.REPEAT_MODE_OFF)

    private val _playerSource = MutableLiveData<Triple<List<MediaItem>, Int, Boolean>>()
    val playerSource: LiveData<Triple<List<MediaItem>, Int, Boolean>> = _playerSource

    var playingHandle = INVALID_HANDLE
    var currentIntent: Intent? = null

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
            .setMediaId(firstPlayNodeName)
            .setTag(firstPlayHandle)
            .build()
        _playerSource.value = Triple(
            listOf(mediaItem),
            if (samePlaylist && firstPlayHandle == playingHandle) 0 else INVALID_VALUE,
            displayNodeNameFirst
        )

        if (intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)) {
            compositeDisposable.add(Completable
                .fromCallable {
                    when (type) {
                        OFFLINE_ADAPTER -> {
                            buildPlaylistFromOfflineNodes(intent, firstPlayHandle)
                        }
                        AUDIO_SEARCH_ADAPTER -> {
                            buildPlaylistFromHandles(intent, firstPlayHandle)
                        }
                        AUDIO_BROWSE_ADAPTER -> {
                            buildPlaylistForAudio(intent, firstPlayHandle)
                        }
                    }
                }
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("AudioPlayerViewModel buildPlayerSource")))
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
                mediaItemFromFile(getOfflineFile(context, it), it.name, it.handle.toLong())
            },
            {
                it.handle.toLong()
            }
        )
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
                    mediaItemFromFile(File(localPath), it.name, it.handle)
                } else if (dbHandler.credentials != null) {
                    MediaItem.Builder()
                        .setUri(Uri.parse(megaApi.httpServerGetLocalLink(it)))
                        .setMediaId(it.name)
                        .setTag(it.handle)
                        .build()
                } else {
                    null
                }
            },
            {
                it.handle
            }
        )
    }

    private fun <T> buildPlaylistFromNodes(
        nodes: List<T>,
        firstPlayHandle: Long,
        validator: (T) -> Boolean,
        mapper: (T) -> MediaItem?,
        handleGetter: (T) -> Long
    ) {
        val mediaItems = ArrayList<MediaItem>()
        var index = 0
        var firstPlayIndex = 0

        for (node in nodes) {
            if (!validator(node)) {
                continue
            }

            val mediaItem = mapper(node) ?: continue
            mediaItems.add(mediaItem)

            if (handleGetter(node) == firstPlayHandle) {
                firstPlayIndex = index
            }
            index++
        }

        if (mediaItems.isNotEmpty()) {
            _playerSource.postValue(Triple(mediaItems, firstPlayIndex, false))
        }
    }

    private fun mediaItemFromFile(file: File, name: String, handle: Long): MediaItem {
        val mediaUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && file.absolutePath.contains(Environment.getExternalStorageDirectory().path)
        ) {
            FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, file)
        } else {
            Uri.fromFile(file)
        }

        return MediaItem.Builder()
            .setUri(mediaUri)
            .setMediaId(name)
            .setTag(handle)
            .build()
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
}
