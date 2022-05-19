package mega.privacy.android.app.generalusecase

import android.content.Intent
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
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
            Single.fromCallable {
                val shareInfo = ShareInfo.processIntent(data, MegaApplication.getInstance())
                shareInfo.ifEmpty { error("Error preparing files") }
                shareInfo[0]
            }

    /**
     * Prepares files to be managed in the app and gets all their info.
     *
     * @param data Intent containing the files to be prepared.
     * @return Single<List<ShareInfo>> List<ShareInfo> with all the file info if everything goes well,
     * onError if not.
     */
    fun prepareFiles(data: Intent): Single<List<ShareInfo>> =
            Single.fromCallable {
                val shareInfo = ShareInfo.processIntent(data, MegaApplication.getInstance())
                shareInfo.ifEmpty { error("Error preparing files") }
            }

    /**
     * Prepares files to be managed in the app and gets all their info.
     *
     * @param files list of FileGalleryItem
     * @return Single<List<ShareInfo>> List<ShareInfo> with all the file info if everything goes well,
     * onError if not.
     */
    fun prepareFilesFromGallery(files: ArrayList<FileGalleryItem>): Single<List<ShareInfo>> =
            Single.fromCallable {
                val shareInfo = ShareInfo.processUploadFile(MegaApplication.getInstance(), files)
                shareInfo.ifEmpty { error("Error preparing files") }
                shareInfo
            }
}