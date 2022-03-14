package mega.privacy.android.app.namecollision

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.myAccount.usecase.GetFileVersionsOptionUseCase
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.namecollision.usecase.GetNameCollisionResultUseCase
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.notifyObserver
import javax.inject.Inject

/**
 * ViewModel which manages data of [NameCollisionActivity]
 */
@HiltViewModel
class NameCollisionViewModel @Inject constructor(
    private val getFileVersionsOptionUseCase: GetFileVersionsOptionUseCase,
    private val getNameCollisionResultUseCase: GetNameCollisionResultUseCase
) : BaseRxViewModel() {

    init {
        getFileVersionsOption()
    }

    enum class NameCollisionType { UPLOAD, COPY, MOVEMENT }

    private val currentCollision: MutableLiveData<NameCollisionResult?> = MutableLiveData()
    private val fileVersioningInfo: MutableLiveData<Triple<Boolean, NameCollisionType, Boolean>> =
        MutableLiveData()

    fun getCurrentCollision(): LiveData<NameCollisionResult?> = currentCollision
    fun getFileVersioningInfo(): LiveData<Triple<Boolean, NameCollisionType, Boolean>> =
        fileVersioningInfo

    private val pendingCollisions: MutableList<NameCollisionResult> = mutableListOf()
    var pendingFileCollisions = 0
    var pendingFolderCollisions = 0

    fun fileVersioningUpdated() {
        fileVersioningInfo.notifyObserver()
    }

    fun setData(collisions: ArrayList<NameCollision>?, collision: NameCollision?) {
        when {
            collisions.isNullOrEmpty() && collision != null -> getCurrentCollision(collision)
            else -> reorderCollisions(collisions!!)
        }
    }

    private fun reorderCollisions(collisions: ArrayList<NameCollision>) {
        getNameCollisionResultUseCase.reorder(collisions)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result ->
                    val reorderedCollisions = result.first
                    pendingFileCollisions = result.second
                    pendingFolderCollisions = result.third
                    getCurrentCollision(reorderedCollisions[0])
                    reorderedCollisions.removeAt(0)
                    getPendingCollisions(reorderedCollisions)
                },
                onError = { error ->
                    logError("No pending collisions", error)
                    currentCollision.value = null
                }
            )
            .addTo(composite)
    }

    private fun getCurrentCollision(collision: NameCollision) {
        var firstUpdate = true

        getNameCollisionResultUseCase.get(collision)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { collisionResult ->
                    currentCollision.value = collisionResult

                    if (firstUpdate) {
                        firstUpdate = false
                        updateFileVersioningInfo()
                    }
                },
                onError = { error ->
                    logError("Error getting collisionResult", error)
                    currentCollision.value = null
                },
                onComplete = { logDebug("Get current name collision finished") }
            )
            .addTo(composite)
    }

    private fun getPendingCollisions(collisions: MutableList<NameCollision>) {
        getNameCollisionResultUseCase.get(collisions)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error -> logError("No pending collisions", error) },
                onNext = { collisionsResult ->
                    pendingCollisions.clear()
                    pendingCollisions.addAll(collisionsResult)
                },
                onComplete = { logDebug("Get complete name collisions finished") }
            )
            .addTo(composite)
    }

    private fun getFileVersionsOption() {
        if (MegaApplication.isDisableFileVersions() == INVALID_VALUE) {
            getFileVersionsOptionUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { logDebug("File versioning: ${MegaApplication.isDisableFileVersions()}") },
                    onError = { error -> LogUtil.logWarning(error.message) })
                .addTo(composite)
        }
    }

    private fun updateFileVersioningInfo() {
        if (currentCollision.value != null) {
            fileVersioningInfo.value =
                Triple(
                    getFileVersioningEnabled(),
                    getNameCollisionType(),
                    currentCollision.value!!.nameCollision.isFile
                )
        }
    }

    private fun getNameCollisionType(): NameCollisionType =
        when (currentCollision.value?.nameCollision) {
            is NameCollision.Copy -> NameCollisionType.COPY
            is NameCollision.Movement -> NameCollisionType.MOVEMENT
            else -> NameCollisionType.UPLOAD
        }

    private fun getFileVersioningEnabled(): Boolean =
        when (MegaApplication.isDisableFileVersions()) {
            0 -> true
            else -> false
        }

    fun replaceUpdateOrMerge(applyOnNext: Boolean) {

    }

    fun cancel(applyOnNext: Boolean) {
        when {
            applyOnNext && pendingFileCollisions > 0 && pendingFolderCollisions > 0 -> {
                while (pendingFileCollisions > 0) {
                    pendingFileCollisions--
                    pendingCollisions.removeAt(0)
                }
                continueWithNext()
            }
            applyOnNext -> currentCollision.value = null
            else -> continueWithNext()
        }
    }

    fun rename(applyOnNext: Boolean) {

    }

    private fun continueWithNext() {
        val nextCollision = pendingCollisions[0]
        pendingCollisions.removeAt(0)
        if (nextCollision.nameCollision.isFile) {
            pendingFileCollisions--
        } else {
            pendingFolderCollisions--
        }
        currentCollision.value = nextCollision
    }
}