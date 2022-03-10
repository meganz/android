package mega.privacy.android.app.namecollision

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.myAccount.usecase.GetFileVersionsOptionUseCase
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.notifyObserver
import javax.inject.Inject

/**
 * ViewModel which manages data of [NameCollisionActivity]
 */
class NameCollisionViewModel @Inject constructor(
    private val getFileVersionsOptionUseCase: GetFileVersionsOptionUseCase
) : BaseRxViewModel() {

    enum class NameCollisionType { UPLOAD, COPY, MOVEMENT }

    private val currentCollision: MutableLiveData<NameCollision> = MutableLiveData()
    private val fileVersioningInfo: MutableLiveData<Triple<Boolean, NameCollisionType, Boolean>> =
        MutableLiveData()

    fun getCurrentCollision(): LiveData<NameCollision> = currentCollision
    fun getFileVersioningInfo(): LiveData<Triple<Boolean, NameCollisionType, Boolean>> =
        fileVersioningInfo

    private val pendingCollisions: MutableList<NameCollision> = mutableListOf()

    fun getPendingCollisions(): Int = pendingCollisions.size

    fun fileVersioningUpdated() {
        fileVersioningInfo.notifyObserver()
    }

    fun setData(collisions: ArrayList<NameCollision>?, collision: NameCollision?) {
        when {
            collisions.isNullOrEmpty() -> pendingCollisions.add(collision!!)
            else -> pendingCollisions.addAll(collisions)
        }

        currentCollision.value = pendingCollisions[0]
        getFileVersionsOption()
    }

    private fun getFileVersionsOption() {
        if (MegaApplication.isDisableFileVersions() != INVALID_VALUE) {
            updateFileVersioningInfo()
            return
        }

        getFileVersionsOptionUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { updateFileVersioningInfo() },
                onError = { error -> LogUtil.logWarning(error.message) })
            .addTo(composite)
    }

    private fun updateFileVersioningInfo() {
        if (currentCollision.value != null) {
            fileVersioningInfo.value =
                Triple(
                    getFileVersioningEnabled(),
                    getNameCollisionType(),
                    currentCollision.value!!.isFile
                )
        }
    }

    private fun getNameCollisionType(): NameCollisionType =
        when (currentCollision.value) {
            is NameCollision.Copy -> NameCollisionType.COPY
            is NameCollision.Movement -> NameCollisionType.MOVEMENT
            else -> NameCollisionType.UPLOAD
        }

    private fun getFileVersioningEnabled(): Boolean =
        when (MegaApplication.isDisableFileVersions()) {
            0 -> true
            else -> false
        }
}