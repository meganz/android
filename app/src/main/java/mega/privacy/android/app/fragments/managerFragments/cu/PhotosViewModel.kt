package mega.privacy.android.app.fragments.managerFragments.cu

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.gallery.repository.PhotosItemRepository
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.RxUtil
import mega.privacy.android.app.utils.ZoomUtil.PHOTO_ZOOM_LEVEL
import nz.mega.sdk.MegaNode
import javax.inject.Inject

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val repository: PhotosItemRepository,
    private val mDbHandler: DatabaseHandler,
    val sortOrderManagement: SortOrderManagement
) : GalleryViewModel(repository, sortOrderManagement) {

    override var mZoom = PHOTO_ZOOM_LEVEL

    fun getOrder() = sortOrderManagement.getOrderCamera()

    private val camSyncEnabled = MutableLiveData<Boolean>()
    private var enableCUShown = false

    fun isEnableCUShown(): Boolean {
        return enableCUShown
    }

    fun setEnableCUShown(shown: Boolean) {
        enableCUShown = shown
    }

    fun isCUEnabled(): Boolean {
        return if (camSyncEnabled.value != null) camSyncEnabled.value!! else false
    }

    fun setInitialPreferences() {
        add(Completable.fromCallable {
            LogUtil.logDebug("setInitialPreferences")
            mDbHandler.setFirstTime(false)
            mDbHandler.setStorageAskAlways(true)
            val defaultDownloadLocation =
                repository.buildDefaultDownloadDir()
            defaultDownloadLocation.mkdirs()
            mDbHandler.setStorageDownloadLocation(
                defaultDownloadLocation.absolutePath
            )
            mDbHandler.isPasscodeLockEnabled = false
            mDbHandler.passcodeLockCode = ""
            val nodeLinks: ArrayList<MegaNode> = repository.getPublicLinks()
            if (nodeLinks.size == 0) {
                LogUtil.logDebug("No public links: showCopyright set true")
                mDbHandler.setShowCopyright(true)
            } else {
                LogUtil.logDebug("Already public links: showCopyright set false")
                mDbHandler.setShowCopyright(false)
            }
            true
        }
            .subscribeOn(Schedulers.io())
            .subscribe(RxUtil.IGNORE, RxUtil.logErr("setInitialPreferences")))
    }

    fun setCamSyncEnabled(enabled: Boolean) {
        add(Completable.fromCallable {
            mDbHandler.setCamSyncEnabled(enabled)
            enabled
        }
            .subscribeOn(Schedulers.io())
            .subscribe(RxUtil.IGNORE, RxUtil.logErr("setCamSyncEnabled")))
    }

    @Suppress("deprecation")
    fun enableCu(enableCellularSync: Boolean, syncVideo: Boolean) {
        add(Completable.fromCallable {
            val localFile = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            )
            mDbHandler.setCamSyncLocalPath(localFile?.absolutePath)
            mDbHandler.setCameraFolderExternalSDCard(false)
            mDbHandler.setCamSyncWifi(!enableCellularSync)
            mDbHandler.setCamSyncFileUpload(
                if (syncVideo) MegaPreferences.PHOTOS_AND_VIDEOS else MegaPreferences.ONLY_PHOTOS
            )
            mDbHandler.setCameraUploadVideoQuality(SettingsConstants.VIDEO_QUALITY_ORIGINAL)
            mDbHandler.setConversionOnCharging(true)
            mDbHandler.setChargingOnSize(SettingsConstants.DEFAULT_CONVENTION_QUEUE_SIZE)
            // After target and local folder setup, then enable CU.
            mDbHandler.setCamSyncEnabled(true)
            camSyncEnabled.postValue(true)
            true
        }
            .subscribeOn(Schedulers.io())
            .subscribe(RxUtil.IGNORE, RxUtil.logErr("enableCu")))
    }

    fun camSyncEnabled(): LiveData<Boolean> {
        add(Single.fromCallable {
            java.lang.Boolean.parseBoolean(
                mDbHandler.preferences?.camSyncEnabled
            )
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                camSyncEnabled.setValue(it)
            }, RxUtil.logErr("camSyncEnabled")))
        return camSyncEnabled
    }
}