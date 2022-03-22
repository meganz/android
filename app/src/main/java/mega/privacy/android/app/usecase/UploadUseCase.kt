package mega.privacy.android.app.usecase

import android.content.Context
import android.content.Intent
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.namecollision.exception.NoPendingCollisionsException
import mega.privacy.android.app.uploadFolder.list.data.UploadFolderResult
import mega.privacy.android.app.usecase.exception.StorageStatePaywallException
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.MegaApiJava
import java.io.File
import javax.inject.Inject

/**
 * Use case for uploading files.
 *
 * @property transfersManagement    Required for checking transfers status.
 */
class UploadUseCase @Inject constructor(
    private val transfersManagement: TransfersManagement
) {
    /**
     * Uploads a file.
     *
     * @param context       Required Context for starting the service.
     * @param absolutePath  Absolute path of the file.
     * @param fileName      Name with which the file has to be uploaded.
     * @param lastModified  Last modified date of the file.
     * @param parentHandle  Handle of the MegaNode in which the file has to be uploaded.
     * @return Completable.
     */
    fun upload(
        context: Context,
        absolutePath: String,
        fileName: String,
        lastModified: Long,
        parentHandle: Long
    ): Completable = Completable.create { emitter ->
        if (MegaApplication.getInstance().storageState == MegaApiJava.STORAGE_STATE_PAYWALL) {
            showOverDiskQuotaPaywallWarning()
            emitter.onError(StorageStatePaywallException())
            return@create
        }

        if (transfersManagement.shouldBreakTransfersProcessing()) {
            emitter.onComplete()
            return@create
        }

        context.startService(
            Intent(context, UploadService::class.java)
                .putExtra(UploadService.EXTRA_FILE_PATH, absolutePath)
                .putExtra(UploadService.EXTRA_NAME, fileName)
                .putExtra(UploadService.EXTRA_LAST_MODIFIED, lastModified / 1000)
                .putExtra(UploadService.EXTRA_PARENT_HASH, parentHandle)
        )

        if (emitter.isDisposed) {
            return@create
        }

        emitter.onComplete()
    }

    /**
     * Uploads a file.
     *
     * @param context       Application Context required to start the service.
     * @param file          File to upload.
     * @param parentHandle  Handle of the MegaNode in which the file has to be uploaded.
     * @return Completable.
     */
    fun upload(context: Context, file: File, parentHandle: Long): Completable =
        upload(
            context = context,
            absolutePath = file.absolutePath,
            fileName = file.name,
            lastModified = file.lastModified(),
            parentHandle = parentHandle
        )

    /**
     * Uploads a file from ShareInfo.
     *
     * @param context       Application Context required to start the service.
     * @param shareInfo     The ShareInfo to upload.
     * @param renameName    A valid name if the file has to be uploaded with other name, null otherwise.
     * @param parentHandle  Handle of the MegaNode in which the file has to be uploaded.
     * @return Completable.
     */
    fun upload(
        context: Context,
        shareInfo: ShareInfo,
        renameName: String? = null,
        parentHandle: Long
    ): Completable =
        upload(
            context = context,
            absolutePath = shareInfo.fileAbsolutePath,
            fileName = renameName ?: shareInfo.title,
            lastModified = shareInfo.lastModified,
            parentHandle = parentHandle
        )

    /**
     * Uploads a file after resolving a name collision.
     *
     * @param context           Application Context required to start the service.
     * @param collisionResult   The result of the name collision.
     * @param rename            True if should rename the file, false otherwise.
     * @return Completable.
     */
    fun upload(
        context: Context,
        collisionResult: NameCollisionResult,
        rename: Boolean
    ): Completable =
        upload(
            context,
            (collisionResult.nameCollision as NameCollision.Upload).absolutePath!!,
            if (rename) collisionResult.renameName!! else collisionResult.nameCollision.name,
            collisionResult.nameCollision.lastModified,
            collisionResult.nameCollision.parentHandle
        )

    /**
     * Uploads a file. Upload folder context.
     *
     * @param context       Application Context required to start the service.
     * @param uploadResult  The result of the upload folder.
     * @return Completable.
     */
    fun upload(
        context: Context,
        uploadResult: UploadFolderResult
    ): Completable =
        upload(
            context,
            uploadResult.absolutePath,
            uploadResult.renameName ?: uploadResult.name,
            uploadResult.lastModified,
            uploadResult.parentHandle
        )

    /**
     * Uploads a list of ShareInfo.
     *
     * @param context       Application Context required to start the service.
     * @param infos         The result of the name collisions.
     * @param nameFiles     Map containing info to rename files if required, null otherwise.
     * @param parentHandle  Handle of the MegaNode in which the file has to be uploaded.
     * @return Completable.
     */
    fun uploadInfos(
        context: Context,
        infos: List<ShareInfo>,
        nameFiles: HashMap<String, String>? = null,
        parentHandle: Long
    ): Completable =
        Completable.create { emitter ->
            if (infos.isEmpty()) {
                emitter.onError(NoPendingCollisionsException())
                return@create
            }

            for (shareInfo in infos) {
                if (emitter.isDisposed) {
                    return@create
                }

                upload(context, shareInfo, nameFiles?.get(shareInfo.getTitle()), parentHandle).blockingSubscribeBy(
                    onError = { error -> logError("Cannot upload.", error) }
                )
            }

            if (emitter.isDisposed) {
                return@create
            }

            emitter.onComplete()
        }

    /**
     * Uploads a list of files after resolving name collisions.
     *
     * @param context       Application Context required to start the service.
     * @param collisions    The result of the name collisions.
     * @param rename        True if should rename the files, false otherwise.
     * @return Completable.
     */
    fun upload(
        context: Context,
        collisions: List<NameCollisionResult>,
        rename: Boolean
    ): Completable =
        Completable.create { emitter ->
            if (collisions.isEmpty()) {
                emitter.onError(NoPendingCollisionsException())
                return@create
            }

            for (collision in collisions) {
                if (emitter.isDisposed) {
                    return@create
                }

                upload(context, collision, rename).blockingSubscribeBy(
                    onError = { error -> logError("Cannot upload.", error) }
                )
            }

            if (emitter.isDisposed) {
                return@create
            }

            emitter.onComplete()
        }
}