package mega.privacy.android.app.utils.permission

import androidx.lifecycle.*

//@MainThread
class PermissionViewModel : ViewModel() {
    private val _permissionRequestResult =
        MutableLiveData<MutableMap<String, PermissionResult>>()
//        get() {
//            field.value ?: run { field.value = mutableMapOf() }
//            return field
//        }

    val permissionRequestResult: LiveData<MutableMap<String, PermissionResult>> =
        _permissionRequestResult

    fun postPermissionRequestResult(map: MutableMap<String, PermissionResult>) {
        _permissionRequestResult.value = map
    }

    inline fun observe(
        owner: LifecycleOwner,
        crossinline requiresPermission: (ArrayList<String>) -> Unit,
        noinline onPermissionDenied: ((ArrayList<String>) -> Unit)?,
        noinline onNeverAskAgain: ((ArrayList<String>) -> Unit)?
    ) {
        permissionRequestResult.observe(owner, Observer { map ->
            map.forEach {
                when (it.value) {
                    PermissionResult.GRANTED -> requiresPermission.invoke(arrayListOf(it.key))
                    PermissionResult.DENIED -> onPermissionDenied?.invoke(arrayListOf(it.key))
                    PermissionResult.DENIED_AND_DISABLED -> onNeverAskAgain?.invoke(arrayListOf(it.key))
                }
            }
        })
    }

    fun removeObservers(owner: LifecycleOwner) = _permissionRequestResult.removeObservers(owner)

    private fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }
}
