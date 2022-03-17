package mega.privacy.android.app.namecollision.usecase

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.namecollision.exception.NoPendingCollisionsException
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.CacheFolderManager.buildThumbnailFile
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo
import mega.privacy.android.app.utils.MegaNodeUtil.getThumbnailFileName
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Use case for getting all required info for a name collision.
 *
 * @property megaApi        MegaApiAndroid instance to ask for thumbnails.
 * @property context        Required for getting thumbnail files.
 * @property getNodeUseCase Required for getting MegaNodes.
 */
class GetNameCollisionResultUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationContext private val context: Context,
    private val getNodeUseCase: GetNodeUseCase
) {

    /**
     * Gets all the required info for present a name collision.
     *
     * @param collision [NameCollision] from which the complete info has to be get.
     * @return Flowable with the recovered info.
     */
    fun get(collision: NameCollision): Flowable<NameCollisionResult> =
        Flowable.create({ emitter ->
            val nameCollisionResult = NameCollisionResult(nameCollision = collision)
            val node = getNodeUseCase.get(collision.collisionHandle).blockingGetOrNull()

            if (node == null) {
                emitter.onError(MegaNodeException.NodeDoesNotExistsException())
                return@create
            }

            with(node) {
                val thumbnailFile = if (hasThumbnail()) buildThumbnailFile(
                    context,
                    getThumbnailFileName()
                ) else null

                nameCollisionResult.apply {
                    collisionName = name
                    collisionSize = if (node.isFile) size else null
                    collisionFolderContent =
                        if (node.isFolder) getMegaNodeFolderInfo(node) else null
                    collisionLastModified = if (node.isFile) modificationTime else creationTime
                    collisionThumbnail =
                        if (thumbnailFile?.exists() == true) thumbnailFile.toUri() else null
                    renameName = collision.name.getRenameName()
                }

                emitter.onNext(nameCollisionResult)

                if (nameCollisionResult.collisionThumbnail != null || !hasThumbnail()) {
                    emitter.onComplete()
                    return@create
                }

                megaApi.getThumbnail(
                    this,
                    thumbnailFile!!.absolutePath,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request: MegaRequest, error: MegaError ->
                            if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                            if (error.errorCode == MegaError.API_OK) {
                                nameCollisionResult.collisionThumbnail = request.file.toUri()
                                emitter.onNext(nameCollisionResult)
                            } else {
                                logWarning(error.toThrowable().stackTraceToString())
                            }

                            emitter.onComplete()
                        }
                    )
                )
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Gets all the required info for present a list of name collisions.
     *
     * @param collisions    List of [NameCollision] from which the complete info has to be get.
     * @return Flowable with the recovered info.
     */
    fun get(collisions: List<NameCollision>): Flowable<MutableList<NameCollisionResult>> =
        Flowable.create({ emitter ->
            val collisionsResult = mutableListOf<NameCollisionResult>()

            for ((i, collision) in collisions.withIndex()) {
                get(collision).blockingSubscribeBy(
                    onNext = { nameCollisionResult ->
                        collisionsResult.add(
                            i,
                            nameCollisionResult
                        )
                    },
                    onError = { error -> logWarning("NameCollisionResult error", error) },
                    onComplete = { emitter.onNext(collisionsResult) }
                )
            }

            if (collisionsResult.isEmpty()) {
                emitter.onError(NoPendingCollisionsException())
            } else {
                emitter.onComplete()
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Reorders a list of [NameCollision] for presenting first files, then folders.
     *
     * @param collisions    List to reorder.
     * @return Single with the reordered list.
     */
    fun reorder(collisions: List<NameCollision>): Single<Triple<MutableList<NameCollision>, Int, Int>> =
        Single.create { emitter ->
            val fileCollisions = mutableListOf<NameCollision>()
            val folderCollisions = mutableListOf<NameCollision>()
            val reorderedCollisions = mutableListOf<NameCollision>()

            for (collision in collisions) {
                if (collision.isFile) {
                    fileCollisions.add(collision)
                } else {
                    folderCollisions.add(collision)
                }
            }

            reorderedCollisions.addAll(fileCollisions)
            reorderedCollisions.addAll(folderCollisions)

            when {
                emitter.isDisposed -> return@create
                reorderedCollisions.isEmpty() -> emitter.onError(NoPendingCollisionsException())
                else -> {
                    val pendingFileCollisions = when {
                        folderCollisions.size > 0 -> fileCollisions.size - 1
                        else -> fileCollisions.size
                    }
                    val pendingFolderCollisions = when (fileCollisions.size) {
                        0 -> folderCollisions.size - 1
                        else -> folderCollisions.size
                    }
                    emitter.onSuccess(
                        Triple(
                            reorderedCollisions,
                            pendingFileCollisions,
                            pendingFolderCollisions
                        )
                    )
                }
            }
        }

    /**
     * Gets the name for rename a collision item in case the user wants to rename it.
     *
     * @return The rename name.
     */
    private fun String.getRenameName(): String {
        var extension = MimeTypeList.typeForName(this).extension
        val pointIndex = lastIndexOf(extension) - 1
        val name = substring(0, pointIndex)
        extension = substring(pointIndex, length)

        val renameName = when {
            name.endsWith("($Int)") -> {
                val firstIndex = lastIndexOf('(')
                var value: Int = name.substring(firstIndex, lastIndexOf(')') + 1).toInt()
                value++
                name.substring(0, firstIndex + 1).plus("$value)")
            }
            else -> name.plus("(1)")
        }

        return renameName.plus(extension)
    }
}