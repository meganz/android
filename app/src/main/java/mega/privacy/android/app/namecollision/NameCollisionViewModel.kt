package mega.privacy.android.app.namecollision

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.domain.entity.NameCollision
import javax.inject.Inject

/**
 * ViewModel which manages data of [NameCollisionActivity]
 */
class NameCollisionViewModel @Inject constructor() : BaseRxViewModel() {

    private val pendingCollisions: MutableList<NameCollision> = mutableListOf()
    private val currentCollision: MutableLiveData<NameCollision> = MutableLiveData()

    fun getCurrentCollision(): LiveData<NameCollision> = currentCollision

    fun getPendingCollisions(): Int = pendingCollisions.size

    fun setData(collisions: ArrayList<NameCollision>?, collision: NameCollision?) {
        when {
            collisions.isNullOrEmpty() -> pendingCollisions.add(collision!!)
            else -> pendingCollisions.addAll(collisions)
        }

        currentCollision.value = pendingCollisions[0]
    }
}