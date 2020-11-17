package mega.privacy.android.app.audioplayer.trackinfo

import android.app.Activity
import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dagger.hilt.android.qualifiers.ActivityContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.audioplayer.service.Metadata
import mega.privacy.android.app.audioplayer.service.MetadataExtractor
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.Constants.FROM_INCOMING_SHARES
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.MegaNodeUtil.getTopAncestorNode
import mega.privacy.android.app.utils.OfflineUtils.*
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder
import mega.privacy.android.app.utils.TimeUtils.formatLongDateTime
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.notifyObserver
import nz.mega.sdk.*
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class TrackInfoViewModel @ViewModelInject constructor(
    private val megaApi: MegaApiAndroid,
    private val dbHandler: DatabaseHandler,
    // we need call legacy code OfflineUtils.saveOffline to save node for offline, which require
    // activity :(
    @ActivityContext private val context: Context,
) : BaseRxViewModel() {
    private val _metadata = MutableLiveData<Pair<Metadata, String>>()
    val metadata: LiveData<Pair<Metadata, String>> = _metadata

    private val _nodeInfo = MutableLiveData<NodeInfo>()
    val nodeInfo: LiveData<NodeInfo> = _nodeInfo

    private var trackInfoArgs: TrackInfoFragmentArgs? = null
    private var metadataOnlyPlayer: Player? = null

    private val createThumbnailRequest = object : BaseListener(context) {
        override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
            if (e.errorCode == MegaError.API_OK) {
                post {
                    _nodeInfo.notifyObserver()
                }
            }
        }
    }

    fun loadTrackInfo(args: TrackInfoFragmentArgs) {
        trackInfoArgs = args

        add(
            Completable
                .fromCallable {
                    loadMetadata(args)
                    loadNodeInfo(args)
                }
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("TrackInfoViewModel loadTrackInfo"))
        )
    }

    private fun loadMetadata(args: TrackInfoFragmentArgs) {
        val trackSelector = DefaultTrackSelector(context)
        val exoPlayer = SimpleExoPlayer.Builder(context, DefaultRenderersFactory(context))
            .setTrackSelector(trackSelector)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    // we only need metadata, so let's set minimum buffer time (1ms)
                    .setBufferDurationsMs(1, 1, 1, 1)
                    .build()
            )
            .build()

        metadataOnlyPlayer = exoPlayer

        exoPlayer.addListener(MetadataExtractor(trackSelector) { title, artist, album ->
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
        if (args.adapterType == OFFLINE_ADAPTER) {
            val node = dbHandler.findByHandle(args.handle) ?: return
            val file = getOfflineFile(context, node)
            if (!file.exists()) {
                return
            }

            val parentName = file.parentFile?.name ?: return
            val grandParentName = file.parentFile?.parentFile?.name
            val location = when {
                grandParentName != null
                        && grandParentName + File.separator + parentName == OFFLINE_INBOX_DIR ->
                    context.getString(R.string.section_saved_for_offline_new)
                parentName == OFFLINE_DIR -> context.getString(R.string.section_saved_for_offline_new)
                else -> parentName + " (" + context.getString(R.string.section_saved_for_offline_new) + ")"
            }

            val thumbnail = getThumbnailFile(context, node)
            createThumbnailIfNotExists(thumbnail, args.handle)

            _nodeInfo.postValue(
                NodeInfo(
                    thumbnail, true, getSizeString(file.length()), location,
                    formatLongDateTime(file.lastModified() / 1000),
                    formatLongDateTime(file.lastModified() / 1000)
                )
            )
        } else {
            val node = megaApi.getNodeByHandle(args.handle) ?: return

            val parent = megaApi.getParentNode(node)
            val topAncestor = getTopAncestorNode(node)
            val location = when {
                args.from == FROM_INCOMING_SHARES -> {
                    if (parent != null) {
                        parent.name + " (" + context.getString(R.string.tab_incoming_shares) + ")"
                    } else {
                        context.getString(R.string.tab_incoming_shares)
                    }
                }
                parent == null -> context.getString(R.string.tab_incoming_shares)
                topAncestor.handle == megaApi.rootNode.handle
                        || topAncestor.handle == megaApi.rubbishNode.handle
                        || topAncestor.handle == megaApi.inboxNode.handle -> {
                    if (topAncestor.handle == parent.handle) {
                        getTranslatedNameForParentNode(topAncestor)
                    } else {
                        parent.name + " (" + getTranslatedNameForParentNode(topAncestor) + ")"
                    }
                }
                else -> parent.name + " (" + context.getString(R.string.tab_incoming_shares) + ")"
            }

            val thumbnail = File(getThumbFolder(context), node.base64Handle.plus(JPG_EXTENSION))
            createThumbnailIfNotExists(thumbnail, args.handle)

            _nodeInfo.postValue(
                NodeInfo(
                    thumbnail, availableOffline(context, node), getSizeString(node.size), location,
                    formatLongDateTime(node.creationTime), formatLongDateTime(node.modificationTime)
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

    private fun getTranslatedNameForParentNode(parent: MegaNode): String {
        return when (parent.handle) {
            megaApi.rootNode.handle -> context.getString(R.string.section_cloud_drive)
            megaApi.rubbishNode.handle -> context.getString(R.string.section_rubbish_bin)
            megaApi.inboxNode.handle -> context.getString(R.string.section_inbox)
            else -> parent.name
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

    fun toggleAvailableOffline(available: Boolean) {
        val args = trackInfoArgs ?: return

        add(
            Completable
                .fromCallable(Callable {
                    if (!available) {
                        removeOffline(dbHandler.findByHandle(args.handle), dbHandler, context)
                    } else {
                        val node = megaApi.getNodeByHandle(args.handle) ?: return@Callable
                        val offlineParent =
                            getOfflineParentFile(context, args.adapterType, node, megaApi)

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

                        saveOffline(offlineParent, node, context, context as Activity)
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("TrackInfoViewModel toggleAvailableOffline"))
        )
    }

    override fun onCleared() {
        super.onCleared()

        metadataOnlyPlayer?.release()
    }
}
