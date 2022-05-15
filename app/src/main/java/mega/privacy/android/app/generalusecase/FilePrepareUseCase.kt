package mega.privacy.android.app.generalusecase

import android.content.Intent
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.utils.StringUtils.toThrowable
import java.util.ArrayList
import javax.inject.Inject

/**
 * Use case which prepares files to managed in the app.
 */
class FilePrepareUseCase @Inject constructor() {

    /**
     * Prepares a file to be managed in the app and gets all its info.
     *
     * @param data Intent containing the file to be prepared.
     * @return Single<ShareInfo> ShareInfo object with all the file info if everything goes well,
     * onError if not.
     */
    fun prepareFile(data: Intent): Single<ShareInfo> =
        Single.create { emitter ->
            val shareInfo = ShareInfo.processIntent(data, MegaApplication.getInstance())

            if (shareInfo.isNotEmpty()) {
                emitter.onSuccess(shareInfo[0])
            } else {
                emitter.onError("Error preparing file".toThrowable())
            }
        }

    /**
     * Prepares files to be managed in the app and gets all their info.
     *
     * @param data Intent containing the files to be prepared.
     * @return Single<List<ShareInfo>> List<ShareInfo> with all the file info if everything goes well,
     * onError if not.
     */
    fun prepareFiles(data: Intent): Single<List<ShareInfo>> =
        Single.create { emitter ->
            val shareInfo = ShareInfo.processIntent(data, MegaApplication.getInstance())

            if (shareInfo.isNotEmpty()) {
                emitter.onSuccess(shareInfo)
            } else {
                emitter.onError("Error preparing files".toThrowable())
            }
        }

    /**
     * Prepares files to be managed in the app and gets all their info.
     *
     * @param files list of FileGalleryItem
     * @return Single<List<ShareInfo>> List<ShareInfo> with all the file info if everything goes well,
     * onError if not.
     */
    fun prepareFilesFromGallery(files: ArrayList<FileGalleryItem>): Single<List<ShareInfo>> =
            Single.create { emitter ->
                val shareInfo = ShareInfo.processUploadFile(MegaApplication.getInstance(), files)

                if (shareInfo.isNotEmpty()) {
                    emitter.onSuccess(shareInfo)
                } else {
                    emitter.onError("Error preparing files".toThrowable())
                }
            }
}