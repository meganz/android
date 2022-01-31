package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_EEXIST
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject

class CreateFolderUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    private fun create(parent: MegaNode, folderName: String): Single<MegaNode> =
        Single.create { emitter ->
            megaApi.createFolder(
                folderName,
                parent,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    if (emitter.isDisposed) {
                        return@OptionalMegaRequestListenerInterface
                    }

                    when (error.errorCode) {
                        API_OK -> emitter.onSuccess(megaApi.getNodeByHandle(request.nodeHandle))
                        API_EEXIST -> emitter.onSuccess(megaApi.getChildNode(parent, folderName))
                        else -> emitter.onError(MegaException(error.errorCode, error.errorString))
                    }
                })
            )
        }

    fun createTree(parent: MegaNode, folderTree: String): Single<MegaNode> =
        Single.create { emitter ->
            var newParent = parent
            val folderNames = folderTree.split(File.separator)

            (if (folderNames.isNullOrEmpty()) listOf(folderTree)
            else folderNames).forEach { folder ->
                create(newParent, folder).blockingSubscribeBy(
                    onError = { error -> emitter.onError(error) },
                    onSuccess = { parentResult -> newParent = parentResult }
                )
            }

            emitter.onSuccess(newParent)
        }
}