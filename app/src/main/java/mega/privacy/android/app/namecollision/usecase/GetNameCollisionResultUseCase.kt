package mega.privacy.android.app.namecollision.usecase

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.utils.CacheFolderManager.buildThumbnailFile
import mega.privacy.android.app.utils.MegaNodeUtil.getThumbnailFileName
import javax.inject.Inject

/**
 * Use case for getting all required info for a name collision.
 */
class GetNameCollisionResultUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getNodeUseCase: GetNodeUseCase
) {

    fun get(collision: NameCollision): Single<NameCollisionResult> =
        Single.create { emitter ->
            val nameCollisionResult = NameCollisionResult(nameCollision = collision)

            getNodeUseCase.get(collision.collisionHandle).blockingSubscribeBy(
                onError = { error -> emitter.onError(error) },
                onSuccess = { node ->
                    with(node) {
                        val thumbnailFile = if (node.hasThumbnail()) buildThumbnailFile(
                            context,
                            node.getThumbnailFileName()
                        ) else null

                        nameCollisionResult.apply {
                            collisionName = name
                            collisionSize = size
                            collisionLastModified = modificationTime
                            collisionThumbnail =
                                if (thumbnailFile?.exists() == true) thumbnailFile.toUri() else null
                        }
                    }
                }
            )
        }
}