package mega.privacy.android.app.usecase

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.usecase.exception.ThumbnailDoesNotExistException
import mega.privacy.android.app.usecase.exception.toMegaException
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeUtil.getThumbnailFileName
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Use case for getting MegaNode thumbnails.
 *
 * @property megaApi        MegaApiAndroid instance for asking the thumbnail if needed.
 * @property context        Required for getting thumbnail files.
 * @property getNodeUseCase Required for getting MegaNodes.
 */
class GetThumbnailUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationContext private val context: Context,
    private val getNodeUseCase: GetNodeUseCase
) {

    /**
     * Gets the thumbnail of a MegaNode.
     *
     * @param handle    The identifier of the MegaNode.
     * @return Single with the Uri of the thumbnail.
     */
    fun get(handle: Long): Single<Uri> =
        Single.create { emitter ->
            if (handle == INVALID_HANDLE) {
                emitter.onError(MegaNodeException.NodeDoesNotExistsException())
                return@create
            }

            get(getNodeUseCase.get(handle).blockingGetOrNull()).blockingSubscribeBy(
                onError = { error -> emitter.onError(error) },
                onSuccess = { result -> emitter.onSuccess(result) }
            )
        }

    /**
     * Gets the thumbnail of a MegaNode.
     *
     * @param node    The MegaNode to get its thumbnail.
     * @return Single with the Uri of the thumbnail.
     */
    fun get(node: MegaNode?): Single<Uri> =
        Single.create { emitter ->
            if (node == null) {
                emitter.onError(MegaNodeException.NodeDoesNotExistsException())
                return@create
            }

            if (!node.hasThumbnail()) {
                emitter.onError(ThumbnailDoesNotExistException())
                return@create
            }

            val thumbnailFile =
                CacheFolderManager.buildThumbnailFile(context, node.getThumbnailFileName())

            if (thumbnailFile?.exists() == true) {
                emitter.onSuccess(thumbnailFile.toUri())
                return@create
            }

            megaApi.getThumbnail(
                node,
                thumbnailFile!!.absolutePath,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request: MegaRequest, error: MegaError ->
                        when {
                            emitter.isDisposed -> return@OptionalMegaRequestListenerInterface
                            error.errorCode == MegaError.API_OK -> emitter.onSuccess(request.file.toUri())
                            else -> emitter.onError(error.toMegaException())
                        }
                    }
                )
            )
        }

    /**
     * Gets the thumbnails of a list of nodes.
     *
     * @param nodes List of nodes to get their thumbnail.
     * @return Flowable<Long> The node handle if the request finished with success, error if not.
     */
    fun get(nodes: List<MegaNode>): Flowable<Long> =
        Flowable.create({ emitter ->
            nodes.forEach { node ->
                if (emitter.isCancelled) {
                    return@create
                }

                get(node).blockingSubscribeBy(
                    onError = { error -> logWarning("No thumbnail.", error) },
                    onSuccess = { emitter.onNext(node.handle) }
                )
            }

            emitter.onComplete()
        }, BackpressureStrategy.LATEST)
}