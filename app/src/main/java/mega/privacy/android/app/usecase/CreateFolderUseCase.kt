package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.MegaException.Companion.toMegaException
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_EEXIST
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject

/**
 * Use case for creating folders.
 *
 * @property megaApi    MegaApiAndroid instance to create folders.
 */
class CreateFolderUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Creates a folder.
     *
     * @param parent        Parent node in which the folder has to be created.
     * @param folderName    Name of the folder to create.
     * @return Single with the MegaNode of the created folder.
     */
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
                        else -> emitter.onError(error.toMegaException())
                    }
                })
            )
        }

    /**
     * Creates a folder tree.
     *
     * @param parent        Parent node in which the folder has to be created.
     * @param folderTree    String containing the folder names tree to create.
     * @return Singled with the MegaNode of the latest created folder of the folder tree.
     */
    fun createTree(parent: MegaNode, folderTree: String): Single<MegaNode> =
        Single.create { emitter ->
            var newParent = parent
            val folderNames = folderTree.split(File.separator)

            (if (folderNames.isNullOrEmpty()) listOf(folderTree)
            else folderNames).forEach { folderName ->
                if (folderName.isNotEmpty()) {
                    create(newParent, folderName).blockingSubscribeBy(
                        onError = { error -> emitter.onError(error) },
                        onSuccess = { parentResult -> newParent = parentResult }
                    )
                }
            }

            emitter.onSuccess(newParent)
        }
}