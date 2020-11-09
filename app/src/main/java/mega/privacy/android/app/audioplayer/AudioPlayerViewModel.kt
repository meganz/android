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

    private val _playerSource = MutableLiveData<Pair<List<MediaItem>, Int>>()
    val playerSource: LiveData<Pair<List<MediaItem>, Int>> = _playerSource

    fun buildPlayerSource(intent: Intent) {
        val type = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val uri = intent.data

        if (type == INVALID_VALUE || uri == null) {
            return
        }

        var playingNodeName = ""
        when (type) {
            OFFLINE_ADAPTER -> {
                val path = intent.getStringExtra(INTENT_EXTRA_KEY_PATH) ?: return
                playingNodeName = File(path).name
            }
            AUDIO_SEARCH_ADAPTER, AUDIO_BROWSE_ADAPTER -> {
                val handle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
                val node = megaApi.getNodeByHandle(handle) ?: return
                playingNodeName = node.name
            }
        }

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(playingNodeName)
            .build()
        _playerSource.value = Pair(listOf(mediaItem), INVALID_VALUE)

        if (intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)) {
            compositeDisposable.add(Completable
                .fromCallable {
                    when (type) {
                        OFFLINE_ADAPTER -> {
                            buildPlaylistFromOfflineNodes(intent)
                        }
                        AUDIO_SEARCH_ADAPTER -> {
                            buildPlaylistFromHandles(intent)
                        }
                        AUDIO_BROWSE_ADAPTER -> {
                            buildPlaylistForAudio(intent)
                        }
                    }
                }
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("AudioPlayerViewModel buildPlayerSource")))
        }
    }

    private fun buildPlaylistFromOfflineNodes(intent: Intent) {
        val nodes = intent.getParcelableArrayListExtra<MegaOffline>(INTENT_EXTRA_KEY_ARRAY_OFFLINE)
            ?: return

        val firstPlayHandle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)

        buildPlaylistFromNodes(
            nodes, firstPlayHandle,
            {
                isFileAvailable(getOfflineFile(context, it))
                        && MimeTypeList.typeForName(it.name).isAudio
                        && !MimeTypeList.typeForName(it.name).isAudioNotSupported
            },
            {
                mediaItemFromFile(getOfflineFile(context, it), it.name)
            },
            {
                it.handle.toLong()
            }
        )
    }

    private fun buildPlaylistFromHandles(intent: Intent) {
        val handles = intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH) ?: return
        buildPlaylistFromHandles(
            handles.toList(), intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
        )
    }

    private fun buildPlaylistForAudio(intent: Intent) {
        val order = intent.getIntExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, ORDER_DEFAULT_ASC)
        buildPlaylistFromNodes(
            megaApi.searchByType(order, FILE_TYPE_AUDIO, SEARCH_TARGET_ROOTNODE),
            intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
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
                    mediaItemFromFile(File(localPath), it.name)
                } else if (dbHandler.credentials != null) {
                    MediaItem.Builder()
                        .setUri(Uri.parse(megaApi.httpServerGetLocalLink(it)))
                        .setMediaId(it.name)
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
            _playerSource.postValue(Pair(mediaItems, firstPlayIndex))
        }
    }

    private fun mediaItemFromFile(file: File, name: String): MediaItem {
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
            .build()
    }

    fun clear() {
        compositeDisposable.dispose()
    }
}
