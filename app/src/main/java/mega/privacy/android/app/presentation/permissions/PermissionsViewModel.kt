package mega.privacy.android.app.presentation.permissions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.data.extensions.filterAllowedPermissions
import mega.privacy.android.app.data.extensions.toPermissionScreen
import mega.privacy.android.app.data.extensions.toPermissionType
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.app.presentation.permissions.model.PermissionScreen
import mega.privacy.android.app.presentation.permissions.model.PermissionType
import mega.privacy.android.app.presentation.permissions.model.Permission
import javax.inject.Inject

/**
 * View model for managing [PermissionsFragment] data.
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor() : ViewModel() {

    private lateinit var missingPermissions: List<Permission>
    private lateinit var permissionScreens: MutableList<PermissionScreen>
    private val showInitialSetupScreen: MutableLiveData<Boolean> = MutableLiveData()
    private val currentPermission: MutableLiveData<PermissionScreen?> = MutableLiveData()
    private val askPermissionType = SingleLiveEvent<PermissionType>()

    /**
     * Checks if should show "Allow access" screen.
     */
    fun shouldShowInitialSetupScreen(): LiveData<Boolean> = showInitialSetupScreen

    /**
     * Notifies about the current [PermissionScreen].
     */
    fun getCurrentPermission(): LiveData<PermissionScreen?> = currentPermission

    /**
     * Notifies about asking for some permissions.
     */
    fun onAskPermission(): SingleLiveEvent<PermissionType> = askPermissionType

    /**
     * Sets initial data for requesting missing permissions.
     */
    fun setData(missingPermissions: List<Pair<Permission, Boolean>>) {
        this.missingPermissions = missingPermissions.filterAllowedPermissions()
            .apply {
                permissionScreens = toPermissionScreen()
            }

        if (permissionScreens.isNotEmpty()) {
            showInitialSetupScreen.value = true
        }
    }

    private fun updateCurrentPermission() {
        if (permissionScreens.isNotEmpty()) {
            currentPermission.value = permissionScreens[0]
        } else {
            currentPermission.value = null
        }
    }

    /**
     * Sets the showInitialSetupScreen value to false and updates the current permission to show.
     */
    fun grantAskForPermissions() {
        showInitialSetupScreen.value = false
        updateCurrentPermission()
    }

    /**
     * Sets next permission to show as the current one.
     */
    fun nextPermission() {
        permissionScreens.removeFirst()
        updateCurrentPermission()
    }

    /**
     * Asks for required permission.
     */
    fun askPermission() {
        askPermissionType.value = currentPermission.value?.toPermissionType(missingPermissions)
    }
}