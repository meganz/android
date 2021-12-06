package mega.privacy.android.app.fragments.managerFragments.cu

import android.content.Context
import android.os.Environment
import android.util.Pair
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.repo.MegaNodeRepo
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import java.util.*

class CuViewModel @ViewModelInject constructor(
    @MegaApi private val mMegaApi: MegaApiAndroid,
    private val mDbHandler: DatabaseHandler,
    private val mRepo: MegaNodeRepo,
    @ApplicationContext private val mAppContext: Context,
    private val mSortOrderManagement: SortOrderManagement
) : GalleryViewModel(mMegaApi, mRepo, mAppContext) {

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
            logDebug("setInitialPreferences")
            mDbHandler.setFirstTime(false)
            mDbHandler.setStorageAskAlways(true)
            val defaultDownloadLocation =
                FileUtil.buildDefaultDownloadDir(mAppContext)
            defaultDownloadLocation.mkdirs()
            mDbHandler.setStorageDownloadLocation(
                defaultDownloadLocation.absolutePath
            )
            mDbHandler.isPasscodeLockEnabled = false
            mDbHandler.passcodeLockCode = ""
            val nodeLinks: ArrayList<MegaNode> = mMegaApi.publicLinks
            if (nodeLinks.size == 0) {
                logDebug("No public links: showCopyright set true")
                mDbHandler.setShowCopyright(true)
            } else {
                logDebug("Already public links: showCopyright set false")
                mDbHandler.setShowCopyright(false)
            }
            true
        }
            .subscribeOn(Schedulers.io())
            .subscribe(IGNORE, logErr("setInitialPreferences")))
    }

    fun setCamSyncEnabled(enabled: Boolean) {
        add(Completable.fromCallable {
            mDbHandler.setCamSyncEnabled(enabled)
            enabled
        }
            .subscribeOn(Schedulers.io())
            .subscribe(IGNORE, logErr("setCamSyncEnabled")))
    }

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
            .subscribe(IGNORE, logErr("enableCu")))
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
            }, logErr("camSyncEnabled")))
        return camSyncEnabled
    }

    override fun getRealMegaNodes(n: MegaNode?): List<Pair<Int, MegaNode>> =
        mRepo.getFilteredCuChildrenAsPairs(mSortOrderManagement.getOrderCamera())

}