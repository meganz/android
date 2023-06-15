package mega.privacy.android.data.listener

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

internal class CreateSetElementListenerInterface(
    private val target: Int,
    private val onCompletion: (success: Int, failure: Int) -> Unit,
) : MegaRequestListenerInterface {
    private var total = 0

    private var success = 0

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        total++
        if (error.errorCode == MegaError.API_OK) {
            success++
        }

        if (total == target) {
            onCompletion(success, total - success)
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError,
    ) {
    }
}

internal class RemoveSetElementListenerInterface(
    private val target: Int,
    private val onCompletion: (success: Int, failure: Int) -> Unit,
) : MegaRequestListenerInterface {
    private var total = 0

    private var success = 0

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        total++
        if (error.errorCode == MegaError.API_OK) {
            success++
        }

        if (total == target) {
            onCompletion(success, total - success)
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError,
    ) {
    }
}

internal class ExportSetsListenerInterface(
    private val totalSets: Int,
    private val onCompletion: (List<Pair<Long, String>>) -> Unit,
) : MegaRequestListenerInterface {
    private val setLinks = mutableListOf<Pair<Long, String>>()

    private var numSets = 0

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        numSets++

        if (error.errorCode == MegaError.API_OK) {
            val set = request.megaSet
            val link = request.link
            if (set != null && link != null) {
                setLinks.add(set.id() to link)
            }
        }

        if (numSets == totalSets) {
            onCompletion(setLinks)
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError,
    ) {
    }
}

internal class DisableExportSetsListenerInterface(
    private val totalSets: Int,
    private val onCompletion: (success: Int, failure: Int) -> Unit,
) : MegaRequestListenerInterface {
    private var numSets = 0

    private var success = 0

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        numSets++

        if (error.errorCode == MegaError.API_OK) success++
        if (numSets == totalSets) onCompletion(success, numSets - success)
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError,
    ) {
    }
}

class GetPreviewElementNodeListenerInterface(
    private val nodeAlbumPhotoIdMap: Map<Long, AlbumPhotoId>,
    private val onCompletion: (List<Pair<MegaNode, AlbumPhotoId>>) -> Unit,
) : MegaRequestListenerInterface {
    private var numRequests = 0

    private val nodeAlbumPhotoIdPairs = mutableListOf<Pair<MegaNode, AlbumPhotoId>>()

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        numRequests++

        if (error.errorCode == MegaError.API_OK) {
            request.publicMegaNode?.also { node ->
                nodeAlbumPhotoIdMap[node.handle]?.also { albumPhotoId ->
                    nodeAlbumPhotoIdPairs.add(node to albumPhotoId)
                }
            }
        }

        if (numRequests == nodeAlbumPhotoIdMap.size) {
            onCompletion(nodeAlbumPhotoIdPairs)
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError,
    ) {
    }
}

internal class CopyPreviewNodeListenerInterface(
    private val nodes: List<MegaNode>,
    private val onCompletion: (List<NodeId>) -> Unit,
) : MegaRequestListenerInterface {
    private var numRequests = 0

    private val nodeIds = mutableListOf<NodeId>()

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        numRequests++

        if (e.errorCode == MegaError.API_OK) {
            request.nodeHandle?.also { handle ->
                nodeIds.add(NodeId(handle))
            }
        }

        if (numRequests == nodes.size) {
            onCompletion(nodeIds)
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {}
}
