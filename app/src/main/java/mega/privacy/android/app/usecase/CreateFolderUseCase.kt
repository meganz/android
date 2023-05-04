package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.exception.toMegaException
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError.API_EEXIST
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for creating folders.
 *
 * @property megaApi    MegaApiAndroid instance to create folders.
 */
class CreateFolderUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) {

    /**
     * Creates a folder.
     *
     * @param parent        Parent node in which the folder has to be created.
     * @param folderName    Name of the folder to create.
     * @return Single with the MegaNode of the created folder.
     */
    fun create(parent: MegaNode?, folderName: String): Single<MegaNode> =
        Single.create { emitter ->
            megaApi.createFolder(
                folderName,
                parent,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    if (emitter.isDisposed) {
                        return@OptionalMegaRequestListenerInterface
                    }

                    val nodeHandle = megaApi.getNodeByHandle(request.nodeHandle)

                    when {
                        error.errorCode == API_OK && nodeHandle != null -> {
                            emitter.onSuccess(nodeHandle)
                        }
                        error.errorCode == API_EEXIST -> megaApi.getChildNode(parent, folderName)
                            ?.let { emitter.onSuccess(it) }
                        else -> emitter.onError(error.toMegaException())
                    }
                })
            )
        }
}