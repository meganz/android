package mega.privacy.android.app.mediaplayer

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.app.mediaplayer.mapper.AudioNodeToMediaItemMapper
import mega.privacy.android.app.mediaplayer.model.AudioPlayQueueParams
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_BUCKET_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil.getDownloadLocation
import mega.privacy.android.app.utils.FileUtil.isFileAvailable

import mega.privacy.android.data.model.MimeTypeList
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetLocalLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.GetParentNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodeByHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesByEmailUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesByHandlesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesByParentHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesFromInSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesFromOutSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesFromPublicLinksUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudiosByParentHandleFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.node.backup.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.AUDIO_BROWSE_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.BACKUPS_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.FAVOURITES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.FILE_BROWSER_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.INCOMING_SHARES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.LINKS_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Builds the audio play queue for the V2 audio player.
 *
 * Emits two [MediaPlaySources] in sequence:
 * 1. An immediate single-item list containing only the requested track so ExoPlayer can start
 *    without waiting for the full play queue fetch.
 * 2. The complete sibling play queue (if [AudioPlayQueueParams.isPlayQueue] is true), resolved
 *    from the appropriate node source with streaming-server setup.
 *
 * The [serverStarted][MediaPlaySources.serverStarted] flag on the second emission tells the
 * caller whether the HTTP streaming server was started and must be stopped on player destroy.
 */
class AudioPlayQueueBuilder @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase,
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase,
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase,
    private val getLocalLinkFromMegaApiUseCase: GetLocalLinkFromMegaApiUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase,
    private val getAudioNodeByHandleUseCase: GetAudioNodeByHandleUseCase,
    private val getAudioNodesUseCase: GetAudioNodesUseCase,
    private val getAudioNodesByParentHandleUseCase: GetAudioNodesByParentHandleUseCase,
    private val getAudioNodesByHandlesUseCase: GetAudioNodesByHandlesUseCase,
    private val getAudioNodesByEmailUseCase: GetAudioNodesByEmailUseCase,
    private val getAudioNodesFromPublicLinksUseCase: GetAudioNodesFromPublicLinksUseCase,
    private val getAudioNodesFromInSharesUseCase: GetAudioNodesFromInSharesUseCase,
    private val getAudioNodesFromOutSharesUseCase: GetAudioNodesFromOutSharesUseCase,
    private val getAudiosByParentHandleFromMegaApiFolderUseCase: GetAudiosByParentHandleFromMegaApiFolderUseCase,
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase,
    private val getRootNodeFromMegaApiFolderUseCase: GetRootNodeFromMegaApiFolderUseCase,
    private val getParentNodeFromMegaApiFolderUseCase: GetParentNodeFromMegaApiFolderUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val audioNodeToMediaItemMapper: AudioNodeToMediaItemMapper,
) {

    /**
     * Build the play queue for [params].
     *
     * Emits a fast single-item [MediaPlaySources] immediately, then (if
     * [AudioPlayQueueParams.isPlayQueue]) emits the full sibling list after all I/O completes.
     * Both emissions run on [IoDispatcher].
     */
    operator fun invoke(params: AudioPlayQueueParams): Flow<MediaPlaySources> = flow {
        val type = params.adapterType
        val handle = params.handle
        val fileName = params.fileName

        // First emit: single item for fast ExoPlayer start.
        val firstUri = if (type == FOLDER_LINK_ADAPTER) {
            if (isMegaApiFolder(type)) {
                getLocalFolderLinkFromMegaApiFolderUseCase(handle)
            } else {
                getLocalFolderLinkFromMegaApiUseCase(handle)
            }?.toUri()
        } else {
            params.uri
        } ?: return@flow

        emit(
            MediaPlaySources(
                mediaItems = listOf(audioNodeToMediaItemMapper(handle, firstUri)),
                newIndexForCurrentItem = 0,
                nameToDisplay = fileName,
            )
        )

        if (!params.isPlayQueue) return@flow

        // Setup streaming server for adapters that require it.
        val serverStarted = if (type != OFFLINE_ADAPTER && type != ZIP_ADAPTER) {
            setupStreamingServer(type)
        } else {
            false
        }

        // Read hidden-node filter state once at invoke time.
        val showHiddenItems = monitorShowHiddenItemsUseCase().first()
        val accountDetail = monitorAccountDetailUseCase().first()
        val accountType = accountDetail.levelDetail?.accountType
        val isPaidAccount = accountType?.isPaid == true
        val isBusinessExpired = if (accountType?.isBusinessAccount == true) {
            runCatching { getBusinessStatusUseCase() }.getOrNull() == BusinessAccountStatus.Expired
        } else {
            false
        }

        val (mediaItems, firstPlayIndex) = buildFullPlayQueue(
            params = params,
            type = type,
            showHiddenItems = showHiddenItems,
            isPaidAccount = isPaidAccount,
            isBusinessExpired = isBusinessExpired,
        )

        if (mediaItems.isNotEmpty()) {
            emit(
                MediaPlaySources(
                    mediaItems = mediaItems,
                    newIndexForCurrentItem = firstPlayIndex,
                    nameToDisplay = null,
                    serverStarted = serverStarted,
                )
            )
        }
    }.flowOn(ioDispatcher)

    private suspend fun buildFullPlayQueue(
        params: AudioPlayQueueParams,
        type: Int,
        showHiddenItems: Boolean,
        isPaidAccount: Boolean,
        isBusinessExpired: Boolean,
    ): Pair<List<MediaItem>, Int> = when (type) {
        OFFLINE_ADAPTER -> buildFromOfflineNodes(params.offlineParentId, params.handle)

        AUDIO_BROWSE_ADAPTER -> buildFromTypedNodes(
            type = type,
            nodes = getAudioNodesUseCase(params.sortOrder),
            handle = params.handle,
            showHiddenItems = showHiddenItems,
            isPaidAccount = isPaidAccount,
            isBusinessExpired = isBusinessExpired,
        )

        FILE_BROWSER_ADAPTER,
        RUBBISH_BIN_ADAPTER,
        BACKUPS_ADAPTER,
        LINKS_ADAPTER,
        INCOMING_SHARES_ADAPTER,
        OUTGOING_SHARES_ADAPTER,
        CONTACT_FILE_ADAPTER,
        FAVOURITES_ADAPTER,
            -> buildFromBrowseAdapter(
            params = params,
            type = type,
            showHiddenItems = showHiddenItems,
            isPaidAccount = isPaidAccount,
            isBusinessExpired = isBusinessExpired,
        )

        RECENTS_ADAPTER, RECENTS_BUCKET_ADAPTER -> {
            val handles = params.handles ?: return Pair(emptyList(), 0)
            buildFromTypedNodes(
                type = type,
                nodes = getAudioNodesByHandlesUseCase(handles),
                handle = params.handle,
                showHiddenItems = showHiddenItems,
                isPaidAccount = isPaidAccount,
                isBusinessExpired = isBusinessExpired,
            )
        }

        FOLDER_LINK_ADAPTER -> buildFromFolderLink(params)

        ZIP_ADAPTER -> buildFromZipFiles(params.zipPath, params.handle)

        SEARCH_BY_ADAPTER -> {
            val searchHandles = params.searchHandles ?: return Pair(emptyList(), 0)
            buildFromTypedNodes(
                type = type,
                nodes = getAudioNodesByHandlesUseCase(searchHandles),
                handle = params.handle,
                showHiddenItems = showHiddenItems,
                isPaidAccount = isPaidAccount,
                isBusinessExpired = isBusinessExpired,
            )
        }

        else -> Pair(emptyList(), 0)
    }

    private suspend fun buildFromOfflineNodes(
        parentId: Int,
        firstPlayHandle: Long,
    ): Pair<List<MediaItem>, Int> = runCatching {
        val list = getOfflineNodesByParentIdUseCase(parentId)
        val mediaItems = mutableListOf<MediaItem>()
        var firstPlayIndex = 0

        list.filter { it.fileTypeInfo is AudioFileTypeInfo && it.fileTypeInfo?.isSupported == true }
            .forEachIndexed { index, item ->
                if (item.handle.toLong() == firstPlayHandle) firstPlayIndex = index
                runCatching { item.absolutePath.toUri() }.onSuccess { uri ->
                    mediaItems.add(audioNodeToMediaItemMapper(item.handle.toLong(), uri))
                }
            }

        mediaItems to firstPlayIndex
    }.getOrElse { e ->
        Timber.e(e, "Failed to build offline play queue")
        emptyList<MediaItem>() to 0
    }

    private suspend fun buildFromBrowseAdapter(
        params: AudioPlayQueueParams,
        type: Int,
        showHiddenItems: Boolean,
        isPaidAccount: Boolean,
        isBusinessExpired: Boolean,
    ): Pair<List<MediaItem>, Int> {
        val parentHandle = params.parentHandle
        val order = params.sortOrder

        val nodes = when (type) {
            LINKS_ADAPTER if parentHandle == INVALID_HANDLE ->
                getAudioNodesFromPublicLinksUseCase(order)

            INCOMING_SHARES_ADAPTER if parentHandle == INVALID_HANDLE ->
                getAudioNodesFromInSharesUseCase(order)

            OUTGOING_SHARES_ADAPTER if parentHandle == INVALID_HANDLE ->
                getAudioNodesFromOutSharesUseCase(lastHandle = INVALID_HANDLE, order = order)

            CONTACT_FILE_ADAPTER if parentHandle == INVALID_HANDLE ->
                params.contactEmail?.let { getAudioNodesByEmailUseCase(it) }

            else -> {
                val parent = if (parentHandle == INVALID_HANDLE) {
                    when (type) {
                        RUBBISH_BIN_ADAPTER -> getRubbishNodeUseCase()
                        BACKUPS_ADAPTER -> getBackupsNodeUseCase()
                        else -> getRootNodeUseCase()
                    }
                } else {
                    getAudioNodeByHandleUseCase(parentHandle)
                }
                parent?.let {
                    getAudioNodesByParentHandleUseCase(
                        parentHandle = it.id.longValue,
                        order = order,
                    )
                }
            }
        } ?: return Pair(emptyList(), 0)

        return buildFromTypedNodes(
            type = type,
            nodes = nodes,
            handle = params.handle,
            showHiddenItems = showHiddenItems,
            isPaidAccount = isPaidAccount,
            isBusinessExpired = isBusinessExpired,
        )
    }

    private suspend fun buildFromFolderLink(
        params: AudioPlayQueueParams,
    ): Pair<List<MediaItem>, Int> {
        val parentHandle = params.parentHandle
        val parent = if (parentHandle == INVALID_HANDLE) {
            getRootNodeFromMegaApiFolderUseCase()
        } else {
            getParentNodeFromMegaApiFolderUseCase(parentHandle)
        } ?: return Pair(emptyList(), 0)

        val children = getAudiosByParentHandleFromMegaApiFolderUseCase(
            parentHandle = parent.id.longValue,
            order = params.sortOrder,
        )
        // Folder-link nodes are not subject to hidden-node filtering.
        return buildFromTypedNodes(
            type = params.adapterType,
            nodes = children,
            handle = params.handle,
            showHiddenItems = false,
            isPaidAccount = false,
            isBusinessExpired = false,
        )
    }

    private fun buildFromZipFiles(
        zipPath: String?,
        firstPlayHandle: Long,
    ): Pair<List<MediaItem>, Int> {
        zipPath ?: return Pair(emptyList(), 0)
        val files = File(zipPath).parentFile?.listFiles()
            ?: return Pair(emptyList(), 0)

        val mediaItems = mutableListOf<MediaItem>()
        var firstPlayIndex = 0

        files.filter { it.isFile && isAudioFile(it.name) }
            .forEachIndexed { index, file ->
                val hashHandle = file.name.hashCode().toLong()
                if (hashHandle == firstPlayHandle) firstPlayIndex = index
                mediaItems.add(audioNodeToMediaItemMapper(hashHandle, file.toUri()))
            }

        return mediaItems to firstPlayIndex
    }

    private suspend fun buildFromTypedNodes(
        type: Int,
        nodes: List<TypedAudioNode>,
        handle: Long,
        showHiddenItems: Boolean,
        isPaidAccount: Boolean,
        isBusinessExpired: Boolean,
    ): Pair<List<MediaItem>, Int> {
        val mediaItems = mutableListOf<MediaItem>()
        var firstPlayIndex = 0

        filterNonSensitiveNodes(nodes, showHiddenItems, isPaidAccount, isBusinessExpired)
            .filterTakeDownNodes()
            .forEachIndexed { index, node ->
                if (node.id.longValue == handle) firstPlayIndex = index

                val localPath = getLocalFilePathUseCase(node)
                val mediaItem = if (localPath != null && isLocalFile(node, localPath)) {
                    audioNodeToMediaItemMapper(node.id.longValue, File(localPath).toUri())
                } else {
                    val url = if (type == FOLDER_LINK_ADAPTER) {
                        if (isMegaApiFolder(type)) {
                            getLocalFolderLinkFromMegaApiFolderUseCase(node.id.longValue)
                        } else {
                            getLocalFolderLinkFromMegaApiUseCase(node.id.longValue)
                        }
                    } else {
                        getLocalLinkFromMegaApiUseCase(node.id.longValue)
                    } ?: return@forEachIndexed
                    audioNodeToMediaItemMapper(node.id.longValue, url.toUri())
                }

                mediaItems.add(mediaItem)
            }

        return mediaItems to firstPlayIndex
    }

    private suspend fun isMegaApiFolder(type: Int): Boolean =
        type == FOLDER_LINK_ADAPTER && !hasCredentialsUseCase()

    private suspend fun setupStreamingServer(type: Int): Boolean =
        if (isMegaApiFolder(type)) {
            if (megaApiFolderHttpServerIsRunningUseCase() != 0) false
            else {
                megaApiFolderHttpServerStartUseCase()
                true
            }
        } else {
            if (megaApiHttpServerIsRunningUseCase() != 0) false
            else {
                megaApiHttpServerStartUseCase()
                true
            }
        }

    private suspend fun isLocalFile(node: TypedFileNode, localPath: String): Boolean {
        if (isOnMegaDownloads(node)) return true
        val fingerprint = node.fingerprint ?: return false
        return runCatching { fingerprint == getFingerprintUseCase(localPath) }.getOrDefault(false)
    }

    private fun isOnMegaDownloads(node: TypedFileNode): Boolean =
        File(getDownloadLocation(), node.name).let { file ->
            isFileAvailable(file) && file.length() == node.size
        }

    private fun filterNonSensitiveNodes(
        nodes: List<TypedAudioNode>,
        showHiddenItems: Boolean,
        isPaidAccount: Boolean,
        isBusinessExpired: Boolean,
    ): List<TypedAudioNode> =
        if (showHiddenItems || !isPaidAccount || isBusinessExpired) {
            nodes
        } else {
            nodes.filter { !it.isMarkedSensitive && !it.isSensitiveInherited }
        }

    private fun List<TypedAudioNode>.filterTakeDownNodes(): List<TypedAudioNode> =
        filter { !it.isTakenDown }

    @Suppress("DEPRECATION")
    private fun isAudioFile(name: String): Boolean =
        MimeTypeList.typeForName(name).let { mime -> mime.isAudio && !mime.isAudioNotSupported }
}
