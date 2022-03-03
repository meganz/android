package mega.privacy.android.app.mediaplayer.trackinfo

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.MegaRequestFinishListener
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.mediaplayer.service.MetadataExtractor
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLocationInfo
import mega.privacy.android.app.utils.OfflineUtils.*
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.ThumbnailUtils.getThumbFolder
import mega.privacy.android.app.utils.TimeUtils.formatLongDateTime
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.notifyObserver
import nz.mega.sdk.MegaApiAndroid
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for track (audio node) info UI logic.
 */
@HiltViewModel
class TrackInfoViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbHandler: DatabaseHandler,
    @ApplicationContext private val context: Context,
) : BaseRxViewModel() {
    private val _metadata = MutableLiveData<Pair<Metadata, String>>()
    val metadata: LiveData<Pair<Metadata, String>> = _metadata

    private val _nodeInfo = MutableLiveData<AudioNodeInfo>()
    val audioNodeInfo: LiveData<AudioNodeInfo> = _nodeInfo

    private var trackInfoArgs: TrackInfoFragmentArgs? = null
    private var metadataOnlyPlayer: Player? = null

    private val _offlineRemoveSnackBarShow = MutableLiveData<Boolean>()
    val offlineRemoveSnackBarShow: LiveData<Boolean> = _offlineRemoveSnackBarShow

    private val createThumbnailRequest = MegaRequestFinishListener({
        _nodeInfo.notifyObserver()
    })

    fun loadTrackInfo(args: TrackInfoFragmentArgs) {
        trackInfoArgs = args

        // ExoPlayer requires API call happens in main thread.
        loadMetadata(args)

        add(
            Completable
                .fromCallable {
                    loadNodeInfo(args)
                }
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("TrackInfoViewModel loadTrackInfo"))
        )
    }

    private fun loadMetadata(args: TrackInfoFragmentArgs) {
        val trackSelector = DefaultTrackSelector(context)
        val exoPlayer = ExoPlayer.Builder(context, DefaultRenderersFactory(context))
            .setTrackSelector(trackSelector)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    // we only need metadata, so let's set minimum buffer time (1ms)
                    .setBufferDurationsMs(1, 1, 1, 1)
                    .build()
            )
            .build()

        metadataOnlyPlayer = exoPlayer

        exoPlayer.addListener(MetadataExtractor { title, artist, album ->
            val duration = exoPlayer.duration
            val durationText = if (duration == C.TIME_UNSET) {
                ""
            } else {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
                "${to2DigitsStr(minutes)}:${to2DigitsStr(seconds)}"
            }

            _metadata.value = Pair(
                Metadata(title, artist, album, exoPlayer.currentMediaItem?.mediaId ?: ""),
                durationText
            )

            exoPlayer.release()
        })

        val nodeName = if (args.adapterType == OFFLINE_ADAPTER) {
            dbHandler.findByHandle(args.handle)?.name ?: ""
        } else {
            megaApi.getNodeByHandle(args.handle)?.name ?: ""
        }
        exoPlayer.setMediaItem(
            MediaItem.Builder()
                .setUri(args.uri)
                .setMediaId(nodeName)
                .build()
        )

        exoPlayer.playWhenReady = false
        exoPlayer.prepare()

        // extracting metadata will take some time, let's display nodeName at first
        // if we are not in offline section.
        if (args.adapterType != OFFLINE_ADAPTER) {
            _metadata.postValue(Pair(Metadata(null, null, null, nodeName), ""))
        }
    }

    private fun to2DigitsStr(value: Long) = if (value < 10) "0$value" else "$value"

    private fun loadNodeInfo(args: TrackInfoFragmentArgs) {
        val location =
            getNodeLocationInfo(args.adapterType, args.fromIncomingShare, args.handle) ?: return

        if (args.adapterType == OFFLINE_ADAPTER) {
            val node = dbHandler.findByHandle(args.handle) ?: return
            val file = getOfflineFile(context, node)
            if (!file.exists()) {
                return
            }

            val thumbnail = getThumbnailFile(context, node)
            createThumbnailIfNotExists(thumbnail, args.handle)

            _nodeInfo.postValue(
                AudioNodeInfo(
                    thumbnail, true, getSizeString(file.length()),
                    location, formatLongDateTime(file.lastModified() / 1000),
                    formatLongDateTime(file.lastModified() / 1000)
                )
            )
        } else {
            val node = megaApi.getNodeByHandle(args.handle) ?: return

            val thumbnail = File(getThumbFolder(context), node.base64Handle.plus(JPG_EXTENSION))
            createThumbnailIfNotExists(thumbnail, args.handle)

            _nodeInfo.postValue(
                AudioNodeInfo(
                    thumbnail, availableOffline(context, node), getSizeString(node.size),
                    location, formatLongDateTime(node.creationTime),
                    formatLongDateTime(node.modificationTime)
                )
            )
        }
    }

    private fun createThumbnailIfNotExists(thumbnail: File, handle: Long) {
        if (!thumbnail.exists() && isOnline(context)) {
            val node = megaApi.getNodeByHandle(handle) ?: return
            megaApi.getThumbnail(node, thumbnail.absolutePath, createThumbnailRequest)
        }
    }

    fun updateNodeNameIfNeeded(handle: Long, newName: String) {
        if (handle == trackInfoArgs?.handle) {
            val meta = _metadata.value ?: return
            _metadata.value = Pair(
                Metadata(meta.first.title, meta.first.artist, meta.first.album, newName),
                meta.second
            )
        }
    }

    /**
     * Make a node available offline, or remove it from offline.
     *
     * @param available whether this node should be available offline
     */
    fun makeAvailableOffline(available: Boolean, activity: FragmentActivity) {
        val args = trackInfoArgs ?: return

        add(
            Completable
                .fromCallable(Callable {
                    if (!available) {
                        removeOffline(dbHandler.findByHandle(args.handle), dbHandler, context)
                    } else {
                        val node = megaApi.getNodeByHandle(args.handle) ?: return@Callable
                        val offlineParent =
                            getOfflineParentFile(activity, args.adapterType, node, megaApi)

                        if (isFileAvailable(offlineParent)) {
                            val offlineFile = File(offlineParent, node.name)
                            // if the file matches to the latest on the cloud, do nothing
                            if (isFileAvailable(offlineFile)
                                && FileUtil.isFileDownloadedLatest(offlineFile, node)
                                && offlineFile.length() == node.size
                            ) {
                                return@Callable
                            }

                            // if the file does not match the latest on the cloud,
                            // delete the old file offline database record
                            val parentName = getOfflineParentFileName(
                                context, node
                            ).absolutePath + File.separator
                            removeOffline(
                                dbHandler.findbyPathAndName(parentName, node.name),
                                dbHandler, context
                            )
                        }

                        // we need call legacy code OfflineUtils.saveOffline to save node for offline, which require
                        // activity :(
                        saveOffline(offlineParent, node, activity)
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    _offlineRemoveSnackBarShow.value = !available
                }, logErr("TrackInfoViewModel toggleAvailableOffline"))
        )
    }

    override fun onCleared() {
        super.onCleared()

        metadataOnlyPlayer?.release()
    }
}
