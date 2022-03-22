package mega.privacy.android.app.namecollision.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.namecollision.exception.NoPendingCollisionsException
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.GetThumbnailUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for getting all required info for a name collision.
 *
 * @property megaApi                MegaApiAndroid instance to ask for thumbnails.
 * @property context                Required for getting thumbnail files.
 * @property getNodeUseCase         Required for getting MegaNodes.
 * @property getThumbnailUseCase    Required for getting thumbnails.
 */
class GetNameCollisionResultUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationContext private val context: Context,
    private val getNodeUseCase: GetNodeUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase
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
            val collisionNode = getNodeUseCase.get(collision.collisionHandle).blockingGetOrNull()

            if (collisionNode == null) {
                emitter.onError(MegaNodeException.NodeDoesNotExistsException())
                return@create
            }

            with(collisionNode) {
                nameCollisionResult.apply {
                    collisionName = name
                    collisionSize = if (collisionNode.isFile) size else null
                    collisionFolderContent =
                        if (collisionNode.isFolder) getMegaNodeFolderInfo(collisionNode) else null
                    collisionLastModified =
                        if (collisionNode.isFile) modificationTime else creationTime
                }

                getRenameName(collision).blockingSubscribeBy(
                    onError = { error -> emitter.onError(error) },
                    onSuccess = { result -> nameCollisionResult.renameName = result }
                )

                emitter.onNext(nameCollisionResult)

                val handle = when {
                    collision is NameCollision.Copy && collision.isFile -> collision.nodeHandle
                    collision is NameCollision.Movement && collision.isFile -> collision.nodeHandle
                    else -> null
                }

                if (handle != null) {
                    getThumbnailUseCase.get(handle).blockingSubscribeBy(
                        onError = { error -> logWarning("No thumbnail", error) },
                        onSuccess = { thumbnailUri ->
                            nameCollisionResult.thumbnail = thumbnailUri
                            emitter.onNext(nameCollisionResult)
                        }
                    )
                }

                if (collision.isFile) {
                    getThumbnailUseCase.get(this).blockingSubscribeBy(
                        onError = { error -> logWarning("No thumbnail", error) },
                        onSuccess = { thumbnailUri ->
                            nameCollisionResult.collisionThumbnail = thumbnailUri
                            emitter.onNext(nameCollisionResult)
                        }
                    )
                }

                emitter.onComplete()
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
                        fileCollisions.size > 0 -> fileCollisions.size - 1
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
     * Updates the rename names of pending collisions based on a rename choice.
     *
     * @param pendingCollisions List of [NameCollisionResult] to update.
     * @param renameNames       List of already applied rename names.
     * @param applyOnNext       True if the choice will be applied for the rest of files, false otherwise.
     * @return Completable.
     */
    fun updateRenameNames(
        pendingCollisions: MutableList<NameCollisionResult>,
        renameNames: MutableList<String>,
        applyOnNext: Boolean
    ): Completable =
        Completable.create { emitter ->
            pendingCollisions.forEach { collision ->
                if (emitter.isDisposed) {
                    return@create
                }

                if (collision.nameCollision.isFile) {
                    var newRenameName = collision.renameName!!

                    getRenameName(collision.nameCollision).blockingSubscribeBy(
                        onError = { error -> emitter.onError(error) },
                        onSuccess = { result -> newRenameName = result }
                    )

                    if (renameNames.contains(newRenameName)) {
                        do {
                            newRenameName = newRenameName.getPossibleRenameName()
                        } while (renameNames.contains(newRenameName))
                    }

                    collision.renameName = newRenameName

                    if (applyOnNext) {
                        renameNames.add(newRenameName)
                    }
                }
            }

            when {
                emitter.isDisposed -> return@create
                else -> emitter.onComplete()
            }
        }

    /**
     * Gets the name for rename a collision item in case the user wants to rename it.
     * Before returning the new name, always check if there is another collision with it.
     *
     * @param collision [NameCollision] from which the rename name has to be get.
     * @return Single with the rename name.
     */
    private fun getRenameName(collision: NameCollision): Single<String> =
        Single.create { emitter ->
            val parentNode =
                if (collision.parentHandle == INVALID_HANDLE) megaApi.rootNode
                else getNodeUseCase.get(collision.parentHandle).blockingGetOrNull()

            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            var newName = collision.name
            var newCollision: MegaNode?
            do {
                if (emitter.isDisposed) {
                    return@create
                }

                newName = newName.getPossibleRenameName()
                newCollision = megaApi.getChildNode(parentNode, newName)
            } while (newCollision != null)

            when {
                emitter.isDisposed -> return@create
                else -> emitter.onSuccess(newName)
            }
        }

    /**
     * Gets a possible name for rename a collision item in case the user wants to rename it.
     *
     * @return The rename name.
     */
    private fun String.getPossibleRenameName(): String {
        var extension = MimeTypeList.typeForName(this).extension
        val pointIndex = lastIndexOf(extension) - 1
        val name = substring(0, pointIndex)
        extension = substring(pointIndex, length)
        val pattern = "\\(\\d+\\)".toRegex()
        val matches = pattern.findAll(name)

        val renameName = when {
            matches.count() > 0 -> {
                val result = matches.last().value
                val number = result.replace("(", "").replace(")", "")
                val newNumber = number.toInt() + 1
                val firstIndex = lastIndexOf('(')
                name.substring(0, firstIndex + 1).plus("$newNumber)")
            }
            else -> name.plus(" (1)")
        }

        return renameName.plus(extension)
    }
}