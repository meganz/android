package mega.privacy.android.app.fragments.managerFragments.cu

import android.content.Context
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.gallery.repository.PhotosItemRepository
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.RxUtil
import mega.privacy.android.app.utils.ZoomUtil.PHOTO_ZOOM_LEVEL
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * TimelineViewModel works with TimelineFragment
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PhotosItemRepository,
    private val mDbHandler: DatabaseHandler,
    val sortOrderManagement: SortOrderManagement,
    private val jobUtilWrapper: JobUtilWrapper,
) : GalleryViewModel(repository, sortOrderManagement) {

    override var mZoom = PHOTO_ZOOM_LEVEL

    /**
     * Get current sort rule from SortOrderManagement
     */
    fun getOrder() = sortOrderManagement.getOrderCamera()

    private val camSyncEnabled = MutableLiveData<Boolean>()
    private var enableCUShown = false

    /**
     * Check is enable CU shown UI
     */
    fun isEnableCUShown(): Boolean {
        return enableCUShown
    }

    /**
     * set enable CU shown UI
     */
    fun setEnableCUShown(shown: Boolean) {
        enableCUShown = shown
    }

    /**
     * Check is CU enabled
     */
    fun isCUEnabled(): Boolean {
        return if (camSyncEnabled.value != null) camSyncEnabled.value!! else false
    }

    /**
     * User enabled Camera Upload, so a periodic job should be scheduled if not already running
     */
    fun startCameraUploadJob() {
        Timber.d("CameraUpload enabled through Photos Tab - fireCameraUploadJob()")
        jobUtilWrapper.fireCameraUploadJob(context, false)
    }

    /**
     * Set Initial Preferences
     */
    fun setInitialPreferences() {
        add(Completable.fromCallable {
            Timber.d("setInitialPreferences")
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
                Timber.d("No public links: showCopyright set true")
                mDbHandler.setShowCopyright(true)
            } else {
                Timber.d("Already public links: showCopyright set false")
                mDbHandler.setShowCopyright(false)
            }
            true
        }
            .subscribeOn(Schedulers.io())
            .subscribe(RxUtil.IGNORE, RxUtil.logErr("setInitialPreferences")))
    }

    /**
     * Set CamSync Enabled to db
     */
    fun setCamSyncEnabled(enabled: Boolean) {
        add(Completable.fromCallable {
            mDbHandler.setCamSyncEnabled(enabled)
            enabled
        }
            .subscribeOn(Schedulers.io())
            .subscribe(RxUtil.IGNORE, RxUtil.logErr("setCamSyncEnabled")))
    }

    fun enableCu(enableCellularSync: Boolean, syncVideo: Boolean) {
        viewModelScope.launch(IO) {
            runCatching {
                enableCUSettingForDb(enableCellularSync, syncVideo)
                startCameraUploadJob()
            }.onFailure {
                Timber.e("enableCu" + it.message)
            }
        }
    }

    /**
     * Enable CU
     */
    @Suppress("deprecation")
    private suspend fun enableCUSettingForDb(enableCellularSync: Boolean, syncVideo: Boolean) {
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
    }

    /**
     * Check camSync Enabled
     *
     * @return camSyncEnabled livedata
     */
    fun camSyncEnabled(): LiveData<Boolean> {
        return camSyncEnabled
    }

    fun checkAndUpdateCamSyncEnabledStatus() {
        viewModelScope.launch(IO) {
            camSyncEnabled.postValue(mDbHandler.preferences?.camSyncEnabled.toBoolean())
        }
    }

}




