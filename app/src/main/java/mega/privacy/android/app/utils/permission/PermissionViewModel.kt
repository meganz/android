package mega.privacy.android.app.utils.permission

import androidx.lifecycle.*

class PermissionViewModel : ViewModel() {
    private val _permissionRequestResult =
        MutableLiveData<MutableMap<String, PermissionResult>>()

    val permissionRequestResult: LiveData<MutableMap<String, PermissionResult>> =
        _permissionRequestResult

    fun postPermissionRequestResult(map: MutableMap<String, PermissionResult>) {
        _permissionRequestResult.value = map
    }

    /**
     * Remove observer and clear MutableMap
     */
    fun removeObservers(owner: LifecycleOwner) {
        permissionRequestResult.removeObservers(owner)
        _permissionRequestResult.value?.clear()
    }

}
