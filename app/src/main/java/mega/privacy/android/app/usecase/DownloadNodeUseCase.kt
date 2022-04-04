package mega.privacy.android.app.usecase

import android.content.Context
import android.content.Intent
import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.DownloadService
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for downloading nodes.
 */
class DownloadNodeUseCase @Inject constructor() {

    /**
     * Downloads a node.
     *
     * @param context   Required for starting service.
     * @param node      [MegaNode] to download.
     * @param localPath Path in which the node will be downloaded.
     * @return Completable.
     */
    fun download(context: Context, node: MegaNode?, localPath: String?): Completable =
        Completable.create { emitter ->
            if (node == null || localPath.isNullOrEmpty()) {
                emitter.onError(IllegalArgumentException("Node or local path not valid."))
                return@create
            }

            context.startService(
                Intent(context, DownloadService::class.java)
                    .putExtra(DownloadService.EXTRA_HASH, node.handle)
                    .putExtra(DownloadService.EXTRA_PATH, localPath)
            )

            emitter.onComplete()
        }
}